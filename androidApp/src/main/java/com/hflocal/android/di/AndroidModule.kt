package com.hflocal.android.di

import android.content.Context
import android.os.Build
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.hflocal.android.ui.screens.auth.AuthViewModel
import com.hflocal.android.ui.screens.catalog.CatalogViewModel
import com.hflocal.android.ui.screens.catalog.ModelDetailViewModel
import com.hflocal.android.ui.screens.chat.ChatViewModel
import com.hflocal.android.ui.screens.downloads.DownloadsViewModel
import com.hflocal.android.ui.screens.models.MyModelsViewModel
import com.hflocal.android.ui.screens.settings.SettingsViewModel
import com.hflocal.shared.data.local.db.HFLocalDatabase
import com.hflocal.shared.domain.model.*
import com.hflocal.shared.domain.repository.IDeviceRepository
import com.hflocal.shared.domain.usecase.*
import kotlinx.coroutines.flow.flowOf
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {

    // SQLDelight driver (platform-specific)
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = HFLocalDatabase.Schema,
            context = androidContext(),
            name = "hflocal.db"
        )
    }

    // Device repository with real hardware detection
    single<IDeviceRepository> {
        val ctx = androidContext()
        val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager

        object : IDeviceRepository {
            private fun buildProfile(): DeviceProfile {
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager?.getMemoryInfo(memoryInfo)

                val totalRamBytes = memoryInfo.totalMem
                val totalRamGb = totalRamBytes / (1024.0 * 1024.0 * 1024.0)
                val cpuArch = Build.SUPPORTED_ABIS.joinToString(", ")
                val isArm64 = cpuArch.contains("arm64", ignoreCase = true) ||
                        cpuArch.contains("aarch64", ignoreCase = true)
                val isArm = cpuArch.contains("arm", ignoreCase = true)
                val isX86 = cpuArch.contains("x86", ignoreCase = true)
                val supportsVulkan = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        ctx.packageManager.hasSystemFeature(
                            android.content.pm.PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL
                        )

                val tier = when {
                    totalRamGb >= 8.0 && isArm64 && supportsVulkan && Build.VERSION.SDK_INT >= 31 ->
                        PerformanceTier.TIER_1_HIGH_END
                    totalRamGb >= 4.0 && isArm64 && Build.VERSION.SDK_INT >= 29 ->
                        PerformanceTier.TIER_2_MID_RANGE
                    totalRamGb >= 3.0 && (isArm64 || isArm) && Build.VERSION.SDK_INT >= 28 ->
                        PerformanceTier.TIER_3_BUDGET
                    totalRamGb < 3.0 || isX86 ->
                        PerformanceTier.TIER_4_UNSUPPORTED
                    else ->
                        PerformanceTier.TIER_3_BUDGET
                }

                return DeviceProfile(
                    socModel = Build.SOC_MODEL.ifEmpty { "Unknown" },
                    cpuCores = Runtime.getRuntime().availableProcessors(),
                    cpuArch = cpuArch,
                    totalRamBytes = totalRamBytes,
                    availableRamBytes = memoryInfo.availMem,
                    supportsVulkan = supportsVulkan,
                    androidSdkVersion = Build.VERSION.SDK_INT,
                    androidVersion = Build.VERSION.RELEASE,
                    tier = tier
                )
            }

            private val cachedProfile = buildProfile()

            override fun getDeviceProfile() = flowOf(cachedProfile)
            override suspend fun refreshDeviceProfile() = buildProfile()
            override suspend fun getCurrentTier() = buildProfile().tier
        }
    }

    // ViewModels
    viewModel { AuthViewModel(get()) }
    viewModel { CatalogViewModel(get(), get()) }
    viewModel { ModelDetailViewModel(get(), get()) }
    viewModel { (modelId: String) ->
        ChatViewModel(
            modelId = modelId,
            createSession = get(),
            addMessage = get(),
            updateMessage = get(),
            updateSession = get(),
            chatRepo = get()
        )
    }

    viewModel {
        MyModelsViewModel(
            getDownloadedModels = get(),
            deleteModel = get()
        )
    }

    viewModel {
        SettingsViewModel(
            getSettings = get(),
            updateSettings = get(),
            logout = get(),
            settingsRepo = get(),
            deviceRepo = get()
        )
    }

    viewModel {
        DownloadsViewModel(
            modelRepo = get()
        )
    }
}
