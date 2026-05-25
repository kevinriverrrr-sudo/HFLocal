package com.hflocal.desktop.repository

import com.hflocal.shared.domain.model.DeviceProfile
import com.hflocal.shared.domain.model.PerformanceTier
import com.hflocal.shared.domain.repository.IDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.lang.management.ManagementFactory

/**
 * Desktop DeviceRepository implementation using JVM system properties
 * and JMX for hardware detection.
 */
class DeviceRepositoryImpl : IDeviceRepository {

    private val profile: DeviceProfile by lazy { detectProfile() }

    override fun getDeviceProfile(): Flow<DeviceProfile> = flowOf(profile)

    override suspend fun refreshDeviceProfile(): DeviceProfile = detectProfile()

    override suspend fun getCurrentTier(): PerformanceTier = profile.tier

    private fun detectProfile(): DeviceProfile {
        val osArch = System.getProperty("os.arch") ?: "unknown"
        val osName = System.getProperty("os.name") ?: "Unknown"
        val osVersion = System.getProperty("os.version") ?: "0.0"
        val cores = Runtime.getRuntime().availableProcessors()

        val jvmMaxMemory = Runtime.getRuntime().maxMemory()

        // Try to get total physical memory via JMX (com.sun.management.OperatingSystemMXBean)
        val totalPhysicalMemory = runCatching {
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val method = osBean.javaClass.getMethod("getTotalPhysicalMemorySize")
            method.invoke(osBean) as Long
        }.getOrDefault(jvmMaxMemory)

        val availableMemory = Runtime.getRuntime().freeMemory()

        val freeStorage = runCatching {
            File(System.getProperty("user.home")).freeSpace
        }.getOrDefault(0L)

        val tier = calculateTier(totalPhysicalMemory, osArch)

        return DeviceProfile(
            socModel = "$osArch CPU",
            cpuCores = cores,
            cpuArch = osArch,
            totalRamBytes = totalPhysicalMemory,
            availableRamBytes = availableMemory,
            freeStorageBytes = freeStorage,
            gpuRenderer = "Desktop GPU",
            supportsVulkan = false,
            androidSdkVersion = 0,
            androidVersion = "$osName $osVersion",
            tier = tier
        )
    }

    private fun calculateTier(ram: Long, arch: String): PerformanceTier {
        val ramGB = ram / (1024 * 1024 * 1024)
        val is64Bit = arch.contains("64") || arch.contains("amd64") || arch.contains("aarch64")

        return when {
            !is64Bit -> PerformanceTier.TIER_4_UNSUPPORTED
            ramGB >= 16 -> PerformanceTier.TIER_1_HIGH_END
            ramGB >= 8 -> PerformanceTier.TIER_2_MID_RANGE
            ramGB >= 4 -> PerformanceTier.TIER_3_BUDGET
            else -> PerformanceTier.TIER_4_UNSUPPORTED
        }
    }
}
