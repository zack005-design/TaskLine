package com.example.taskfoundation.reminders

import android.app.*
import android.content.*
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.example.taskfoundation.MainActivity
import com.example.taskfoundation.TaskFoundationApplication
import com.example.taskfoundation.R
import com.example.taskfoundation.data.local.TaskDatabase
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.domain.time.TaskSchedule
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One OS alarm always points at the earliest unconsumed reminder in Room. */
class TaskReminders(private val context: Context, private val db: TaskDatabase) {
    private val mutex = Mutex()
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val alarms = context.getSystemService(AlarmManager::class.java)

    init { notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT)) }

    fun enabled(): Boolean = notifications.areNotificationsEnabled() &&
        notifications.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    private fun alarm() = PendingIntent.getBroadcast(context, 0,
        Intent(context, ReminderReceiver::class.java).setAction("REMIND"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    suspend fun reconcile(deliver: Boolean = false) = mutex.withLock {
        val tasks = db.taskDao().all().map { it.toDomain() }
        notifications.activeNotifications.filter { it.tag?.startsWith("task:") == true }.forEach { active ->
            val task = tasks.find { "task:${it.id}" == active.tag }
            if (task == null || task.isCompleted || TaskSchedule.reminderAt(task) != active.notification.extras.getLong("reminderAt")) {
                notifications.cancel(active.tag, 1)
            }
        }
        alarms.cancel(alarm())
        if (!enabled()) return@withLock
        val now = System.currentTimeMillis()
        val candidates = tasks.mapNotNull { task ->
            TaskSchedule.reminderAt(task)?.takeIf { it != task.lastNotifiedAt }?.let { task to it }
        }.sortedBy { it.second }
        for ((task, at) in candidates) {
            if (!deliver || at > now) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, maxOf(at, now + 1000), alarm())
                break
            }
            // Serialize the final read/receipt with task edits and restores.
            db.withTransaction {
            val fresh = db.taskDao().get(task.id)?.toDomain() ?: return@withTransaction
            if (TaskSchedule.reminderAt(fresh) != at || fresh.lastNotifiedAt == at) return@withTransaction
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .setData(Uri.parse("taskline://task/${task.id}"))
                .putExtra("taskId", task.id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.taskline_mark).setContentTitle(fresh.title)
                .setContentText("Task reminder").setContentIntent(open).setAutoCancel(true)
                .addExtras(android.os.Bundle().apply { putLong("reminderAt", at) })
                .addAction(Notification.Action.Builder(null, "Complete", action(task.id, at, "COMPLETE")).build())
                .addAction(Notification.Action.Builder(null, "Snooze 10 min", action(task.id, at, "SNOOZE")).build())
                .build()
            notifications.notify("task:${task.id}", 1, notification)
            db.taskDao().markNotified(task.id, at)
            }
        }
    }

    private fun action(id: Long, at: Long, action: String) = PendingIntent.getBroadcast(context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(action)
            .setData(Uri.parse("taskline://reminder/$id/$at/$action"))
            .putExtra("taskId", id).putExtra("at", at), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    suspend fun handle(action: String?, id: Long, at: Long) {
        mutex.withLock {
            if (db.runReminderAction(action, id, at)) notifications.cancel("task:$id", 1)
        }
        reconcile(deliver = action == "REMIND")
    }

    companion object { const val CHANNEL = "task-reminders" }
}

private suspend fun TaskDatabase.runReminderAction(action: String?, id: Long, at: Long): Boolean =
    withTransaction {
        val task = taskDao().get(id)?.toDomain()
        if (task != null && TaskSchedule.reminderAt(task) == at && action in setOf("COMPLETE", "SNOOZE")) {
            val now = System.currentTimeMillis()
            if (action == "COMPLETE") taskDao().completeScheduled(id, true, now)
            if (action == "SNOOZE") taskDao().snooze(id, now + 600_000, now)
            true
        } else false
    }

open class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8000) {
                    (context.applicationContext as TaskFoundationApplication).container.reminders.handle(
                        intent.action, intent.getLongExtra("taskId", -1), intent.getLongExtra("at", -1))
                }
            } catch (error: Exception) {
                Log.e("TaskReminders", "Could not process reminder; will retry on next app launch", error)
            } finally { pending.finish() }
        }
    }
}

class ReminderRecoveryReceiver : ReminderReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        super.onReceive(context, intent)
    }
}
