package com.hflocal.android.di

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
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {

    // SQLDelight driver (platform-specific)
    single<SqlDriver> {
        try {
            AndroidSqliteDriver(
                schema = HFLocalDatabase.Schema,
                context = androidContext(),
                name = "hflocal.db"
            )
        } catch (e: Exception) {
            // Delete corrupted DB and retry once
            androidContext().deleteDatabase("hflocal.db")
            AndroidSqliteDriver(
                schema = HFLocalDatabase.Schema,
                context = androidContext(),
                name = "hflocal.db"
            )
        }
    }

    // Device repository with real hardware detection
    single<IDeviceRepository> {
        com.hflocal.android.repository.DeviceRepositoryImpl(androidContext())
    }

    // ViewModels
    viewModel { AuthViewModel(get()) }
    viewModel { CatalogViewModel(get()) }
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
            deleteModelUseCase = get()
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
