package com.example.todoapplication.notification

import android.net.Uri
import com.example.todoapplication.domain.model.Reminder
import java.security.MessageDigest

/** TODO와 Reminder ID 조합으로 Alarm URI와 Notification ID의 안정적인 identity를 만든다. */
object AlarmIdentity {
    fun uri(reminder: Reminder): Uri = Uri.Builder()
        .scheme(SCHEME)
        .authority(AUTHORITY)
        .appendPath(reminder.todoId.toString())
        .appendPath(reminder.id.toString())
        .build()

    fun notificationId(reminder: Reminder): Int {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("${reminder.todoId}:${reminder.id}".toByteArray())
        return (bytes[0].toInt() and 0xff shl 24) or
            (bytes[1].toInt() and 0xff shl 16) or
            (bytes[2].toInt() and 0xff shl 8) or
            (bytes[3].toInt() and 0xff)
    }

    private const val SCHEME = "todo-reminder"
    private const val AUTHORITY = "alarm"
}
