package com.example.todoapplication.app

import android.app.Application
import com.example.todoapplication.notification.NotificationChannels

/** 앱 프로세스 수명 동안 Notification channel과 단일 [AppContainer]를 초기화한다. */
class TodoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        container = DefaultAppContainer(this)
    }
}
