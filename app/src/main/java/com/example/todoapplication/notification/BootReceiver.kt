package com.example.todoapplication.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoapplication.app.TodoApplication
import com.example.todoapplication.application.service.ReminderRecoveryService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver(
    private val serviceProvider: (Context) -> ReminderRecoveryService = { context ->
        (context.applicationContext as TodoApplication).container.reminderRecoveryService
    },
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val recoveryService = try {
            serviceProvider(context)
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            pendingResult.finish()
            return
        }
        launchRecovery(
            recoveryService = recoveryService,
            dispatcher = dispatcher,
            onFinished = pendingResult::finish,
        )
    }
}

internal fun launchRecovery(
    recoveryService: ReminderRecoveryService,
    dispatcher: CoroutineDispatcher,
    onFinished: () -> Unit,
) {
    CoroutineScope(dispatcher).launch {
        try {
            recoveryService.restoreScheduledReminders()
        } catch (_: RuntimeException) {
            // A boot-time recovery failure must not crash the application process.
        } finally {
            onFinished()
        }
    }
}
