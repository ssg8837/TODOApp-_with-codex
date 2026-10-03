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

/**
 * `BOOT_COMPLETED`만 처리해 재부팅 후 Reminder Alarm 복구를 시작하는 BroadcastReceiver.
 *
 * DB나 AlarmManager에 직접 접근하지 않고 AppContainer의 [ReminderRecoveryService]에 위임한다.
 * [goAsync]로 Receiver 수명을 연장하고 정상·실패 경로 모두에서 `finish()`를 보장하며,
 * Activity 또는 UI를 시작하지 않는다.
 */
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

/** 복구 실패를 앱 프로세스 crash로 만들지 않으면서 PendingResult를 반드시 종료한다. */
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
