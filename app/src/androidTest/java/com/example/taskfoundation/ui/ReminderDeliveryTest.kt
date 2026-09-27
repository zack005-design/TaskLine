package com.example.taskfoundation.ui

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.repository.OfflineTaskRepository
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.CalendarDates
import com.example.taskfoundation.domain.time.SystemClock
import com.example.taskfoundation.reminders.TaskReminders
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReminderDeliveryTest {
    @Test fun deliverySnoozeAndCompletionRespectCurrentDatabaseState() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val permissionWasGranted = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        fun shell(command: String) = instrumentation.uiAutomation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() }
        }
        if (Build.VERSION.SDK_INT >= 33 && !permissionWasGranted) shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        val db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        val reminders = TaskReminders(context, db)
        val repository = OfflineTaskRepository(db.taskDao(), SystemClock)
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            assertTrue(reminders.enabled())
            val id = repository.saveTask(Task(title = "Reminder integration test", createdAt = 0, updatedAt = 0,
                dueDateTime = CalendarDates.encode(LocalDate.now().minusDays(1)), dueTimeMinutes = 600, reminderMinutes = 0))
            reminders.reconcile(deliver = true)
            val at = db.taskDao().get(id)!!.lastNotifiedAt!!
            assertTrue(manager.activeNotifications.any { it.tag == "task:$id" })
            reminders.handle("SNOOZE", id, at)
            assertTrue(db.taskDao().get(id)!!.snoozedUntil!! > System.currentTimeMillis())
            assertFalse(manager.activeNotifications.any { it.tag == "task:$id" })
            // The old notification's Complete action must not complete the snoozed occurrence.
            reminders.handle("COMPLETE", id, at)
            assertFalse(db.taskDao().get(id)!!.isCompleted)
            val nextDelivery = System.currentTimeMillis() - 1000
            db.taskDao().snooze(id, nextDelivery, System.currentTimeMillis())
            reminders.reconcile(deliver = true)
            assertTrue(manager.activeNotifications.any { it.tag == "task:$id" })
            reminders.handle("COMPLETE", id, at)
            assertFalse(db.taskDao().get(id)!!.isCompleted)
            assertTrue("An obsolete action must not dismiss the newer notification", manager.activeNotifications.any { it.tag == "task:$id" })
            reminders.handle("COMPLETE", id, nextDelivery)
            assertTrue(db.taskDao().get(id)!!.isCompleted)
            assertFalse(manager.activeNotifications.any { it.tag == "task:$id" })
        } finally {
            db.backupDao().deleteTasks()
            reminders.reconcile()
            db.close()
            // Permission is isolated to this development test install and is reset by uninstall.
        }
    }
}
