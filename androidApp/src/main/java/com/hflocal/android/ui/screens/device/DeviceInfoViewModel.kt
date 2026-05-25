package com.hflocal.android.ui.screens.device

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.DeviceProfile
import com.hflocal.shared.domain.model.PerformanceTier
import com.hflocal.shared.domain.repository.IDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceInfoState(
    val deviceProfile: DeviceProfile = DeviceProfile(),
    val tier: PerformanceTier = PerformanceTier.TIER_4_UNSUPPORTED,
    val totalRam: String = "Unknown",
    val availableRam: String = "Unknown",
    val freeStorage: String = "Unknown",
    val totalStorage: String = "Unknown",
    val cpuInfo: String = "Unknown",
    val gpuRenderer: String = "Unknown",
    val supportsVulkan: Boolean = false,
    val androidVersion: String = "Unknown",
    val sdkVersion: Int = 0,
    val isLoading: Boolean = true,
    val maxModelSize: String = "N/A",
    val error: String? = null
)

class DeviceInfoViewModel(
    private val deviceRepository: IDeviceRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceInfoState())
    val state: StateFlow<DeviceInfoState> = _state.asStateFlow()

    init {
        refreshDevice()
    }

    fun refreshDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val info = collectDeviceInfo()
                val tier = calculateTier(info)
                val profile = DeviceProfile(
                    socModel = info.socModel,
                    cpuCores = info.cpuCores,
                    cpuArch = info.cpuArch,
                    totalRamBytes = info.totalRamBytes,
                    availableRamBytes = info.availableRamBytes,
                    freeStorageBytes = info.freeStorageBytes,
                    gpuRenderer = info.gpuRenderer,
                    supportsVulkan = info.supportsVulkan,
                    androidSdkVersion = info.sdkVersion,
                    androidVersion = info.androidVersion,
                    tier = tier
                )

                val maxModelSizeBytes = tier.maxModelSizeBytes
                val maxModelSize = formatModelSize(maxModelSizeBytes)

                _state.value = DeviceInfoState(
                    deviceProfile = profile,
                    tier = tier,
                    totalRam = formatBytes(info.totalRamBytes),
                    availableRam = formatBytes(info.availableRamBytes),
                    freeStorage = formatBytes(info.freeStorageBytes),
                    totalStorage = formatBytes(info.totalStorageBytes),
                    cpuInfo = "${info.cpuCores} cores · ${info.cpuArch}",
                    gpuRenderer = info.gpuRenderer,
                    supportsVulkan = info.supportsVulkan,
                    androidVersion = "${info.androidVersion} (API ${info.sdkVersion})",
                    sdkVersion = info.sdkVersion,
                    isLoading = false,
                    maxModelSize = maxModelSize
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed to detect device info")
            }
        }
    }

    private fun collectDeviceInfo(): RawDeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        // Total and available RAM
        val memoryInfo = ActivityManager.MemoryInfo()
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo)
        } else {
            // Use Runtime memory as fallback
            memoryInfo.totalMem = Runtime.getRuntime().maxMemory()
            memoryInfo.availMem = Runtime.getRuntime().freeMemory()
        }
        val totalRamBytes = memoryInfo.totalMem
        val availableRamBytes = memoryInfo.availMem

        // Free storage using StatFs
        var freeStorageBytes = 0L
        var totalStorageBytes = 0L
        try {
            val path = context.getExternalFilesDir(null) ?: context.filesDir
            val stat = StatFs(path.absolutePath)
            freeStorageBytes = stat.availableBlocksLong * stat.blockSizeLong
            totalStorageBytes = stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {
            // Fallback: try data directory
            try {
                val stat = StatFs(Environment.getDataDirectory().path)
                freeStorageBytes = stat.availableBlocksLong * stat.blockSizeLong
                totalStorageBytes = stat.blockCountLong * stat.blockSizeLong
            } catch (_: Exception) { }
        }

        // GPU info — GLES20.glGetString requires a GL context which is not available
        // on Dispatchers.IO. Use Build properties as a fallback.
        // A TextureView / GLSurfaceView callback would be needed for real GPU info.
        val gpuRenderer = Build.SOC_MODEL.ifEmpty { "Unknown" }

        // Vulkan support check
        val supportsVulkan = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)

        return RawDeviceInfo(
            socModel = Build.SOC_MODEL.ifEmpty { "Unknown" },
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuArch = Build.SUPPORTED_ABIS.joinToString(", "),
            totalRamBytes = totalRamBytes,
            availableRamBytes = availableRamBytes,
            freeStorageBytes = freeStorageBytes,
            totalStorageBytes = totalStorageBytes,
            gpuRenderer = gpuRenderer,
            supportsVulkan = supportsVulkan,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT
        )
    }

    private fun calculateTier(info: RawDeviceInfo): PerformanceTier {
        val totalRamGb = info.totalRamBytes / (1024.0 * 1024.0 * 1024.0)
        val isArm64 = info.cpuArch.contains("arm64", ignoreCase = true) ||
                info.cpuArch.contains("aarch64", ignoreCase = true)
        val isArm = info.cpuArch.contains("arm", ignoreCase = true)
        val isX86 = info.cpuArch.contains("x86", ignoreCase = true)

        return when {
            totalRamGb >= 8.0 && isArm64 && info.supportsVulkan && info.sdkVersion >= 31 ->
                PerformanceTier.TIER_1_HIGH_END
            totalRamGb >= 4.0 && isArm64 && info.sdkVersion >= 29 ->
                PerformanceTier.TIER_2_MID_RANGE
            totalRamGb >= 3.0 && (isArm64 || isArm) && info.sdkVersion >= 28 ->
                PerformanceTier.TIER_3_BUDGET
            totalRamGb < 3.0 || isX86 ->
                PerformanceTier.TIER_4_UNSUPPORTED
            else ->
                PerformanceTier.TIER_3_BUDGET
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format("%.1f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.0f MB", mb)
        }
    }

    private fun formatModelSize(bytes: Long): String {
        if (bytes <= 0) return "N/A"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }
}

/**
 * Holds raw device info before tier calculation.
 */
private data class RawDeviceInfo(
    val socModel: String = "",
    val cpuCores: Int = 0,
    val cpuArch: String = "",
    val totalRamBytes: Long = 0,
    val availableRamBytes: Long = 0,
    val freeStorageBytes: Long = 0,
    val totalStorageBytes: Long = 0,
    val gpuRenderer: String = "",
    val supportsVulkan: Boolean = false,
    val androidVersion: String = "",
    val sdkVersion: Int = 0
)

class DeviceInfoViewModelFactory(
    private val deviceRepository: IDeviceRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceInfoViewModel::class.java)) {
            return DeviceInfoViewModel(deviceRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
