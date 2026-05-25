package com.hflocal.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.hflocal.shared.data.local.db.HFLocalDatabase
import com.hflocal.shared.data.remote.HuggingFaceApi
import com.hflocal.shared.data.remote.createHttpClient
import com.hflocal.shared.data.repository.*
import com.hflocal.shared.domain.repository.*
import com.hflocal.shared.domain.usecase.*
import io.ktor.client.HttpClient
import org.koin.dsl.module

val sharedModule = module {
    // ── Database ─────────────────────────────────────────────────────
    // SqlDriver is provided by platform modules (androidModule / desktopModule)
    single { HFLocalDatabase(get<SqlDriver>()) }

    // ── Network ─────────────────────────────────────────────────────
    single<HttpClient> { createHttpClient() }
    single { HuggingFaceApi(get()) }
    single<IHuggingFaceRepository> { HuggingFaceRepositoryImpl(get()) }

    // ── Repositories ───────────────────────────────────────────────
    single<IModelRepository> { ModelRepositoryImpl(get()) }
    single<IChatRepository> { ChatRepositoryImpl(get()) }
    single<ISettingsRepository> { SettingsRepositoryImpl(get()) }

    // ── Use Cases ───────────────────────────────────────────────────
    factory { SearchModelsUseCase(get(), get()) }
    factory { GetModelDetailsUseCase(get()) }
    factory { GetDownloadedModelsUseCase(get()) }
    factory { DeleteModelUseCase(get()) }
    factory { DownloadModelUseCase(get(), get()) }
    factory { GetChatSessionsUseCase(get()) }
    factory { CreateChatSessionUseCase(get()) }
    factory { UpdateChatSessionUseCase(get()) }
    factory { UpdateSessionUseCase(get()) }
    factory { AddMessageUseCase(get()) }
    factory { UpdateMessageUseCase(get()) }
    factory { SendMessageUseCase(get()) }
    factory { GetMessagesUseCase(get()) }
    factory { DeleteChatSessionUseCase(get()) }
    factory { GetDeviceProfileUseCase(get()) }
    factory { LoginWithTokenUseCase(get(), get()) }
    factory { LogoutUseCase(get()) }
    factory { GetSettingsUseCase(get()) }
    factory { UpdateSettingsUseCase(get()) }
}
