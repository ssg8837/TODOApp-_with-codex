package com.example.todoapplication.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapplication.R
import com.example.todoapplication.domain.model.Reminder
import com.example.todoapplication.domain.model.Todo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationChannelsIntegrationTest {

    @Test
    fun manifestDeclaresVibratePermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        assertTrue(
            "Manifest must declare android.permission.VIBRATE",
            requestedPermissions.contains(Manifest.permission.VIBRATE),
        )
    }

    @Test
    fun createConfiguresNotificationChannelWithVibration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationChannels.create(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(NotificationChannels.TODO_REMINDERS)
            assertNotNull("Channel must be created", channel)
            assertEquals(NotificationChannels.TODO_REMINDERS, channel.id)
            assertEquals(context.getString(R.string.notification_channel_name), channel.name.toString())
            assertEquals(context.getString(R.string.notification_channel_description), channel.description)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
            assertTrue("Vibration should be enabled", channel.shouldVibrate())
            assertArrayEquals(NotificationChannels.DEFAULT_VIBRATION_PATTERN, channel.vibrationPattern)
        }
    }

    @Test
    fun notificationFactoryUsesMigratedChannel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val todo = Todo(
            id = 10L,
            title = "Test TODO",
            date = LocalDate.now(),
            time = LocalTime.NOON,
            categoryId = 1L,
            isCompleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val reminder = Reminder(
            id = 20L,
            todoId = 10L,
            minutesBefore = Reminder.FIFTEEN_MINUTES_BEFORE,
        )

        val notification = NotificationFactory.create(context, todo, reminder)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertEquals(NotificationChannels.TODO_REMINDERS, notification.channelId)
        }
    }
}

