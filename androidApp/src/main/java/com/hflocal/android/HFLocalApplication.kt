package com.hflocal.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import app.cash.sqldelight.db.SqlDriver
import com.hflocal.android.di.androidModule
import com.hflocal.shared.di.sharedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class HFLocalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global crash handler — write crash info to file for debugging
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val log = StringBuilder()
            log.append("=== CRASH LOG ===\n")
            log.append("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
            log.append("Thread: ${thread.name}\n")
            log.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (SDK ${android.os.Build.VERSION.SDK_INT})\n")
            log.append("\nStack Trace:\n")
            log.append(android.util.Log.getStackTraceString(throwable))
            log.append("\n=== END ===\n")
            try {
                val file = java.io.File(filesDir, "crash_log.txt")
                file.writeText(log.toString())
            } catch (_: Exception) {}
            // Let the system handle the crash normally
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }

        // Initialize Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@HFLocalApplication)
            modules(sharedModule, androidModule)
        }

        // Create notification channels
        createNotificationChannels()

        // Pre-warm database on IO thread to avoid main-thread I/O later
        CoroutineScope(Dispatchers.IO).launch {
            try {
                org.koin.mp.KoinPlatform.getKoin().get<SqlDriver>()
            } catch (_: Exception) { /* handled by AndroidModule retry logic */ }
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Download channel
        val downloadChannel = NotificationChannel(
            CHANNEL_DOWNLOAD,
            "Model Downloads",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows download progress for AI models"
            enableVibration(false)
        }

        // System channel
        val systemChannel = NotificationChannel(
            CHANNEL_SYSTEM,
            "System Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "System alerts and errors"
        }

        manager.createNotificationChannel(downloadChannel)
        manager.createNotificationChannel(systemChannel)
    }

    companion object {
        const val CHANNEL_DOWNLOAD = "channel_download"
        const val CHANNEL_SYSTEM = "channel_system"
    }
}
