package com.hflocal.android.repository

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.hflocal.shared.domain.model.DeviceProfile
import com.hflocal.shared.domain.model.PerformanceTier
import com.hflocal.shared.domain.repository.IDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Full Android DeviceRepository implementation with real hardware detection.
 * Requires [Context] for accessing ActivityManager, PackageManager, etc.
 */
class DeviceRepositoryImpl(private val context: Context) : IDeviceRepository {

    private var cachedProfile: DeviceProfile? = null

    override fun getDeviceProfile(): Flow<DeviceProfile> = flowOf(getProfile())

    override suspend fun refreshDeviceProfile(): DeviceProfile {
        cachedProfile = detectProfile()
        return cachedProfile!!
    }

    override suspend fun getCurrentTier(): PerformanceTier = getProfile().tier

    private fun getProfile(): DeviceProfile {
        cachedProfile?.let { return it }
        cachedProfile = detectProfile()
        return cachedProfile!!
    }

    private fun detectProfile(): DeviceProfile {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem
        val availableRam = memoryInfo.availMem
        val cores = Runtime.getRuntime().availableProcessors()
        val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val soc = Build.SOC_MODEL.ifEmpty { "Unknown" }

        val freeStorage = runCatching {
            val path = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = android.os.StatFs(path.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        }.getOrDefault(0L)

        val supportsVulkan = context.packageManager.hasSystemFeature(
            PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL
        )

        val tier = calculateTier(totalRam, arch, supportsVulkan, Build.VERSION.SDK_INT)

        return DeviceProfile(
            socModel = soc,
            cpuCores = cores,
            cpuArch = arch,
            totalRamBytes = totalRam,
            availableRamBytes = availableRam,
            freeStorageBytes = freeStorage,
            gpuRenderer = "Android GPU",
            supportsVulkan = supportsVulkan,
            androidSdkVersion = Build.VERSION.SDK_INT,
            androidVersion = Build.VERSION.RELEASE,
            tier = tier
        )
    }

    private fun calculateTier(
        ram: Long,
        arch: String,
        vulkan: Boolean,
        sdk: Int
    ): PerformanceTier {
        val ramGB = ram / (1024 * 1024 * 1024)
        val isArm64 = arch.contains("arm64") || arch.contains("aarch64")
        val isArm = arch.contains("arm") || isArm64

        return when {
            !isArm -> PerformanceTier.TIER_4_UNSUPPORTED
            ramGB >= 8 && isArm64 && vulkan && sdk >= 31 -> PerformanceTier.TIER_1_HIGH_END
            ramGB >= 4 && isArm64 && sdk >= 29 -> PerformanceTier.TIER_2_MID_RANGE
            ramGB >= 3 && isArm && sdk >= 28 -> PerformanceTier.TIER_3_BUDGET
            else -> PerformanceTier.TIER_4_UNSUPPORTED
        }
    }
}
