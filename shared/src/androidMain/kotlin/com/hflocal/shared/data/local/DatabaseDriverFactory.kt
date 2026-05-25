package com.hflocal.shared.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.hflocal.shared.data.local.db.HFLocalDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = HFLocalDatabase.Schema,
            context = context,
            name = "hflocal.db"
        )
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        return driver
    }
}
