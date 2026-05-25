package com.hflocal.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.hflocal.android.di.androidModule
import com.hflocal.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class HFLocalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@HFLocalApplication)
            modules(sharedModule, androidModule)
        }

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Download channel
        val downloadChannel = NotificationChannel(
            CHANNEL_DOWNLOAD,
            "Model Downloads",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows download progress for AI models"
            enableVibration(false)
        }

        // System channel
        val systemChannel = NotificationChannel(
            CHANNEL_SYSTEM,
            "System Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "System alerts and errors"
        }

        manager.createNotificationChannel(downloadChannel)
        manager.createNotificationChannel(systemChannel)
    }

    companion object {
        const val CHANNEL_DOWNLOAD = "channel_download"
        const val CHANNEL_SYSTEM = "channel_system"
    }
}
