package com.hflocal.desktop.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.hflocal.shared.data.local.db.HFLocalDatabase
import com.hflocal.shared.domain.model.DeviceProfile
import com.hflocal.shared.domain.model.PerformanceTier
import com.hflocal.shared.domain.repository.IDeviceRepository
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.module
import java.io.File

val desktopModule = module {

    // SQLDelight driver (platform-specific)
    single<SqlDriver> {
        val dbPath = File(System.getProperty("user.home"), ".hflocal/hflocal.db")
        dbPath.parentFile?.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.absolutePath}")
        HFLocalDatabase.Schema.create(driver)
        driver
    }

    // Device repository for desktop
    single<IDeviceRepository> {
        object : IDeviceRepository {
            val profile = DeviceProfile(
                socModel = "Desktop CPU",
                cpuCores = Runtime.getRuntime().availableProcessors(),
                cpuArch = System.getProperty("os.arch"),
                totalRamBytes = Runtime.getRuntime().maxMemory(),
                availableRamBytes = Runtime.getRuntime().freeMemory(),
                freeStorageBytes = File(System.getProperty("user.home")).freeSpace,
                tier = PerformanceTier.TIER_1_HIGH_END,
                androidVersion = System.getProperty("os.name") + " " + System.getProperty("os.version"),
                androidSdkVersion = 0,
                supportsVulkan = false
            )

            override fun getDeviceProfile() = flowOf(profile)
            override suspend fun refreshDeviceProfile() = profile
            override suspend fun getCurrentTier() = profile.tier
        }
    }
}
