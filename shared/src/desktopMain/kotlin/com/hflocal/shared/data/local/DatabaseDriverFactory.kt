package com.hflocal.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.hflocal.shared.data.local.db.HFLocalDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbPath = File(System.getProperty("user.home"), ".hflocal/hflocal.db")
        dbPath.parentFile?.mkdirs()
        val driver: SqlDriver
        if (dbPath.exists()) {
            driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.absolutePath}")
        } else {
            driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.absolutePath}")
            HFLocalDatabase.Schema.create(driver)
        }
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        return driver
    }
}
