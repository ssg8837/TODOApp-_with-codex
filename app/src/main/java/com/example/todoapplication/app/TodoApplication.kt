package com.example.todoapplication.app

import android.app.Application
import com.example.todoapplication.notification.NotificationChannels

class TodoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        container = DefaultAppContainer(this)
    }
}
