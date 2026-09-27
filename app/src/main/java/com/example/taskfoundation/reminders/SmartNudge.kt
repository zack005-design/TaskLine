package com.example.taskfoundation.reminders

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.*
import com.example.taskfoundation.MainActivity
import com.example.taskfoundation.R
import com.example.taskfoundation.TaskFoundationApplication
import com.example.taskfoundation.data.mapper.toDomain
import com.example.taskfoundation.domain.stats.TaskStats
import java.util.concurrent.TimeUnit

object ProductivityNotifications {
    fun post(context: Context, channel: String, channelName: String, title: String, message: String, taskId: Long? = null) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_DEFAULT))
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannel(channel).importance == NotificationManager.IMPORTANCE_NONE) return
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .setData(android.net.Uri.parse("taskline://$channel/${taskId ?: 0}"))
        taskId?.let { intent.putExtra("taskId", it) }
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            manager.notify(channel, 1, Notification.Builder(context, channel).setSmallIcon(R.drawable.taskline_mark)
                .setContentTitle(title).setContentText(message).setStyle(Notification.BigTextStyle().bigText(message))
                .setContentIntent(pending).setAutoCancel(true).build())
        } catch (_: SecurityException) {
            // Permission can be revoked between the check and delivery.
        }
    }
}

class SmartNudge(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        val database = (applicationContext as TaskFoundationApplication).container.database
        val tasks = database.taskDao().all().map { it.toDomain() }
        val count = TaskStats.dailyCompletions(tasks).sumOf { it.completed }
        if (count > 0) ProductivityNotifications.post(applicationContext, "weekly-progress", "Weekly progress",
            "Your week in TaskLine", "$count completed ${if (count == 1) "task was" else "tasks were"} updated this week. Keep it up!")
        Result.success()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        android.util.Log.e("TaskLineNudge", "Unable to read weekly progress", error)
        Result.retry()
    }

    companion object {
        fun schedule(context: Context) = WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "taskline-weekly-progress", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SmartNudge>(7, TimeUnit.DAYS).setInitialDelay(7, TimeUnit.DAYS).build())
    }
}
