package com.hflocal.android.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hflocal.android.MainActivity
import com.hflocal.android.HFLocalApplication
import com.hflocal.shared.domain.model.DownloadedModel
import com.hflocal.shared.domain.repository.IModelRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readAvailable
import kotlinx.coroutines.coroutineScope
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * A [CoroutineWorker] that downloads a single model file from HuggingFace
 * using Ktor's streaming API so that progress can be reported in real time.
 *
 * ### Input data keys
 * - `KEY_MODEL_ID`       – e.g. "TheBloke/llama-2-7b-gguf"
 * - `KEY_FILE_NAME`      – e.g. "llama-2-7b.Q4_K_M.gguf"
 * - `KEY_AUTHOR`         – e.g. "TheBloke"
 * - `KEY_DOWNLOAD_URL`   – full URL to the blob
 * - `KEY_EXPECTED_SHA`   – optional SHA‑256 of the file (logged only)
 * - `KEY_RESUME_OFFSET`  – byte offset to resume from (for pause/resume)
 *
 * ### Output data keys
 * - `KEY_MODEL_ID`  ( echoed )
 * - `KEY_FILE_PATH`  – absolute path on disk
 * - `KEY_ERROR`      – error message if the download failed
 */
class DownloadService(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        // ── Input / output keys ───────────────────────────────────────────
        const val KEY_MODEL_ID      = "model_id"
        const val KEY_FILE_NAME     = "file_name"
        const val KEY_AUTHOR        = "author"
        const val KEY_DOWNLOAD_URL  = "download_url"
        const val KEY_EXPECTED_SHA  = "expected_sha"
        const val KEY_RESUME_OFFSET = "resume_offset"

        // ── Output keys ───────────────────────────────────────────────────
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR     = "error"

        // ── Notification ──────────────────────────────────────────────────
        /** Derive a deterministic, per-download notification ID to avoid collisions
         *  (BUG-01 fix: each model gets its own unique notification ID). */
        private const val NOTIFICATION_BASE = 1000
        private fun notificationId(modelId: String): Int =
            (modelId.hashCode() and 0x7FFFFFFF) % 50000 + NOTIFICATION_BASE

        /** Build a deterministic unique work name so we can cancel / resume by model. */
        fun uniqueWorkName(modelId: String): String = "download_$modelId"

        // ── Public enqueue helper ─────────────────────────────────────────

        /**
         * Enqueue a download work request and return the generated [Operation].
         *
         * @param modelId     HuggingFace model identifier (e.g. "TheBloke/llama-2-7b-gguf")
         * @param fileName    Filename of the GGUF file to download
         * @param author      Author / org name (used for the directory structure)
         * @param downloadUrl Full resolve URL from [HuggingFaceApi.getDownloadUrl]
         * @param expectedSha Optional SHA‑256 hex digest for post‑download verification
         */
        fun enqueue(
            context: Context,
            modelId: String,
            fileName: String,
            author: String,
            downloadUrl: String,
            expectedSha: String? = null
        ): Operation {
            val data = workDataOf(
                KEY_MODEL_ID     to modelId,
                KEY_FILE_NAME    to fileName,
                KEY_AUTHOR       to author,
                KEY_DOWNLOAD_URL to downloadUrl,
                KEY_EXPECTED_SHA to expectedSha
            )
            // BUG-04 fix: require network connectivity so retries don't fire while offline
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<DownloadService>()
                .setInputData(data)
                .setConstraints(constraints)
                .build()

            return WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    uniqueWorkName(modelId),
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }

    // ── Koin‑injected dependencies (late‑init because CoroutineWorker ─────
    //   receives context/params via constructor, not Koin) ──────────────────
    private val httpClient: HttpClient by inject(HttpClient::class.java)
    private val modelRepository: IModelRepository by inject(IModelRepository::class.java)

    // ── Pause flag (checked between buffer reads) ──────────────────────────
    @Volatile
    private var paused = false

    // ── Downloaded bytes so far (across retries within the same work) ──────
    private var downloadedBytes: Long = 0L

    // ═══════════════════════════════════════════════════════════════════════
    //  Main entry point
    // ═══════════════════════════════════════════════════════════════════════

    override suspend fun doWork(): Result = coroutineScope {
        val modelId     = inputData.getString(KEY_MODEL_ID)     ?: return@coroutineScope Result.failure()
        val fileName    = inputData.getString(KEY_FILE_NAME)    ?: return@coroutineScope Result.failure()
        val author      = inputData.getString(KEY_AUTHOR)       ?: ""
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL) ?: return@coroutineScope Result.failure()
        val expectedSha = inputData.getString(KEY_EXPECTED_SHA)
        val resumeFrom  = inputData.getLong(KEY_RESUME_OFFSET, 0L)

        // BUG-13 fix: strip author prefix from modelId to avoid nested dirs like
        // models/TheBloke/TheBloke/llama-2-7b-gguf/
        val safeModelName = modelId.removePrefix("$author/")

        val modelDir = File(context.getExternalFilesDir(null), "models/$author/$safeModelName")
        // BUG-08 fix: check mkdirs() return value
        if (!modelDir.exists() && !modelDir.mkdirs()) {
            throw IOException("Cannot create directory: ${modelDir.absolutePath}")
        }
        val targetFile = File(modelDir, fileName)

        try {
            // ── Download with progress ────────────────────────────────────
            val (totalBytes, finalPath) = downloadFile(
                url         = downloadUrl,
                targetFile  = targetFile,
                modelId     = modelId,
                fileName    = fileName,
                resumeFrom  = resumeFrom,
                expectedSha = expectedSha
            )

            // ── Update repository to mark as completed ────────────────────
            modelRepository.saveModel(
                DownloadedModel(
                    modelId         = modelId,
                    author          = author,
                    fileName        = fileName,
                    filePath        = finalPath,
                    fileSizeBytes   = totalBytes,
                    downloadDate    = System.currentTimeMillis(),
                    isDownloaded    = true,
                    downloadProgress = 1.0f
                )
            )

            // ── Show completion notification ───────────────────────────────
            showCompletionNotification(modelId, fileName)

            Result.success(
                workDataOf(
                    KEY_MODEL_ID  to modelId,
                    KEY_FILE_PATH to finalPath
                )
            )
        } catch (e: Exception) {
            if (paused) {
                // Pause = we stopped intentionally → save offset for resume
                Result.success(
                    workDataOf(KEY_MODEL_ID to modelId)
                )
            } else {
                // BUG-09 fix: clean up partial file on failure when not resuming
                if (resumeFrom == 0L) targetFile.delete()
                modelRepository.updateDownloadProgress(modelId, -1f) // signal error
                showFailureNotification(modelId, fileName, e.message ?: "Unknown error")
                Result.retry()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Streaming download
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun downloadFile(
        url: String,
        targetFile: File,
        modelId: String,
        fileName: String,
        resumeFrom: Long,
        expectedSha: String?
    ): Pair<Long, String> {
        setForeground(createForegroundInfo(modelId, fileName, 0))

        val statement: HttpStatement = httpClient.prepareGet(url) {
            // Resume support via Range header
            if (resumeFrom > 0L) {
                downloadedBytes = resumeFrom
                header(HttpHeaders.Range, "bytes=$resumeFrom-")
            }
        }

        // BUG-14 fix: close response on status check failure
        val response = statement.execute()
        try {
            if (response.status != HttpStatusCode.OK &&
                response.status != HttpStatusCode.PartialContent
            ) {
                throw IllegalStateException("HTTP ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            response.close()
            throw e
        }

        val contentLength = response.contentLength() ?: -1L
        val totalSize = if (resumeFrom > 0L) resumeFrom + contentLength else contentLength
        val append = resumeFrom > 0L

        // Track speed / ETA
        var speedWindowStart = System.currentTimeMillis()
        var speedWindowBytes: Long = 0
        var lastSpeedBps: Long = 0
        // BUG-02 fix: throttle setForeground() to once per second
        var lastNotifUpdate = 0L

        val channel: ByteReadChannel = response.body()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        if (!append) {
            downloadedBytes = 0L
        }

        // When resuming, open in append mode
        val fos = if (append) {
            targetFile.appendOutputStream()
        } else {
            targetFile.outputStream()
        }

        fos.use { outputStream ->
            while (!channel.isClosedForRead) {
                if (paused) {
                    // Pause requested — stop cleanly
                    return@downloadFile Pair(downloadedBytes, targetFile.absolutePath)
                }

                val read = channel.readAvailable(buffer)
                if (read <= 0) continue

                outputStream.write(buffer, 0, read)
                downloadedBytes += read
                speedWindowBytes += read

                // Calculate speed every ~1 second
                val now = System.currentTimeMillis()
                val elapsed = now - speedWindowStart
                if (elapsed >= 1_000L) {
                    lastSpeedBps = (speedWindowBytes * 1_000L) / elapsed.coerceAtLeast(1L)
                    speedWindowStart = now
                    speedWindowBytes = 0L
                }

                // Report progress
                val progress = if (totalSize > 0L) {
                    (downloadedBytes.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                modelRepository.updateDownloadProgress(modelId, progress)

                // BUG-02 fix: throttle setForeground() to once per second to avoid
                // excessive IPC calls that cause UI jank and battery drain
                if (now - lastNotifUpdate >= 1_000L) {
                    setForeground(
                        createForegroundInfo(
                            modelId     = modelId,
                            fileName    = fileName,
                            progress   = progress,
                            speedBps   = lastSpeedBps,
                            downloaded = downloadedBytes,
                            total      = totalSize
                        )
                    )
                    lastNotifUpdate = now
                }
            }
        }

        // ── SHA‑256 verification ───────────────────────────────────────────
        if (expectedSha != null) {
            try {
                val sha = sha256Hex(targetFile)
                if (sha.equals(expectedSha, ignoreCase = true)) {
                    android.util.Log.d("DownloadService", "SHA‑256 verified: $sha")
                } else {
                    // BUG-12 fix: delete corrupted file and throw instead of just logging
                    android.util.Log.w(
                        "DownloadService",
                        "SHA‑256 mismatch! expected=$expectedSha got=$sha"
                    )
                    targetFile.delete()
                    throw IOException("SHA-256 mismatch for $fileName: expected=$expectedSha got=$sha")
                }
            } catch (e: IOException) {
                throw e  // re-throw SHA mismatch
            } catch (e: Exception) {
                android.util.Log.w(
                    "DownloadService",
                    "SHA‑256 check failed: ${e.message}"
                )
            }
        }

        return Pair(
            if (totalSize > 0L) totalSize else downloadedBytes,
            targetFile.absolutePath
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Notification helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun createForegroundInfo(
        modelId: String,
        fileName: String,
        progress: Float,
        speedBps: Long = 0L,
        downloaded: Long = 0L,
        total: Long = 0L
    ): ForegroundInfo {
        val percent = (progress * 100).roundToInt()

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedText = formatSpeed(speedBps)
        val sizeText = if (total > 0L) {
            "${formatBytes(downloaded)} / ${formatBytes(total)}"
        } else {
            formatBytes(downloaded)
        }

        val notification: Notification = NotificationCompat
            .Builder(context, HFLocalApplication.CHANNEL_DOWNLOAD)
            .setContentTitle("Downloading: $fileName")
            .setContentText("$percent%  •  $sizeText  •  $speedText")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, progress <= 0f)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        // BUG-01 fix: use per-model notification ID to avoid collisions
        return ForegroundInfo(notificationId(modelId), notification)
    }

    private fun showCompletionNotification(modelId: String, fileName: String) {
        val notificationManager = context.getSystemService(
            NotificationManager::class.java
        )
        val notification = NotificationCompat
            .Builder(context, HFLocalApplication.CHANNEL_DOWNLOAD)
            .setContentTitle("Download complete")
            .setContentText("$fileName finished downloading")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        // BUG-01 fix: use per-model notification ID
        notificationManager.notify(notificationId(modelId) + 1, notification)
    }

    private fun showFailureNotification(modelId: String, fileName: String, error: String) {
        val notificationManager = context.getSystemService(
            NotificationManager::class.java
        )
        val notification = NotificationCompat
            .Builder(context, HFLocalApplication.CHANNEL_SYSTEM)
            .setContentTitle("Download failed")
            .setContentText("$fileName — $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        // BUG-01 fix: use per-model notification ID
        notificationManager.notify(notificationId(modelId) + 2, notification)
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Utility helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val exp = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val idx = exp.coerceIn(0, units.lastIndex)
        return String.format(
            "%.1f %s",
            bytes / Math.pow(1024.0, idx.toDouble()),
            units[idx]
        )
    }

    private fun formatSpeed(bps: Long): String {
        return when {
            bps < 1_000       -> "$bps B/s"
            bps < 1_000_000   -> String.format("%.1f KB/s", bps / 1_000.0)
            bps < 1_000_000_000 -> String.format("%.1f MB/s", bps / 1_000_000.0)
            else              -> String.format("%.2f GB/s", bps / 1_000_000_000.0)
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
