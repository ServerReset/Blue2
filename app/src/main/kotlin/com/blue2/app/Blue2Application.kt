package com.blue2.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Blue2Application : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_COMMANDS,
                    "Vehicle Commands",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Notifications for vehicle commands (lock, start, etc.)" }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_AUTO_LOCK,
                    "Auto Lock",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Walk-away auto lock notifications" }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_STATUS,
                    "Vehicle Status",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Background vehicle status updates" }
            )
        }
    }

    companion object {
        const val CHANNEL_COMMANDS = "commands"
        const val CHANNEL_AUTO_LOCK = "auto_lock"
        const val CHANNEL_STATUS = "status"
    }
}
