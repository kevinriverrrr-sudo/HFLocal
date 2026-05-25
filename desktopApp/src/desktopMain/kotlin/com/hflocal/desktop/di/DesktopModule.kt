package com.hflocal.desktop.di

import app.cash.sqldelight.db.SqlDriver
import com.hflocal.shared.data.local.DatabaseDriverFactory
import com.hflocal.desktop.repository.DeviceRepositoryImpl
import com.hflocal.shared.domain.repository.IDeviceRepository
import org.koin.dsl.module

val desktopModule = module {

    // SQLDelight driver (platform-specific) — delegates to DatabaseDriverFactory
    single<SqlDriver> {
        DatabaseDriverFactory().createDriver()
    }

    // Device repository for desktop
    single<IDeviceRepository> { DeviceRepositoryImpl() }
}
