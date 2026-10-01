package com.example.taskfoundation.focus

import android.content.Context
import androidx.work.*
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.reminders.ProductivityNotifications
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Persists the one active session so a recreated activity/process can recover it. */
class FocusStore(context: Context, preferencesName: String = "focus-session") {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun read(now: Long = System.currentTimeMillis()): FocusState = synchronized(lock) {
        val id = prefs.getString("session", null) ?: return@synchronized FocusState()
        val total = prefs.getInt("total", 1500)
        val end = prefs.getLong("end", 0)
        FocusState(prefs.getBoolean("active", false), prefs.getLong("task", 0),
            prefs.getString("title", "") ?: "", remainingSeconds(end, now, total), total, id, end)
    }

    fun start(task: Task, minutes: Int): FocusState = synchronized(lock) {
        require(minutes in 1..180)
        val id = UUID.randomUUID().toString()
        val total = minutes * 60
        check(prefs.edit().putString("session", id).putLong("task", task.id).putString("title", task.title)
            .putInt("total", total).putLong("end", System.currentTimeMillis() + total * 1000L)
            .putBoolean("active", true).commit()) { "Could not save the focus session" }
        read()
    }

    fun schedule(state: FocusState): Operation? = synchronized(lock) {
        val current = read()
        // A cancelled restore/start must never replace a newer session's worker.
        if (current.sessionId != state.sessionId || !current.isActive) return@synchronized null
        val work = OneTimeWorkRequestBuilder<FocusCompletionWorker>()
            .setInputData(workDataOf("session" to state.sessionId))
            .setInitialDelay((state.endsAt - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, work)
    }

    fun complete(id: String) = synchronized(lock) {
        val state = read()
        if (state.sessionId != id || !state.isActive || state.remainingSeconds > 0) return@synchronized
        ProductivityNotifications.post(context, "focus", "Focus sessions", "Focus session complete",
            "You focused on ${state.taskTitle}. Time for a break.", state.taskId)
        check(prefs.edit().putBoolean("active", false).commit()) { "Could not finish the focus session" }
    }

    fun stop(): Operation = synchronized(lock) {
        check(prefs.edit().clear().commit()) { "Could not stop the focus session" }
        context.getSystemService(android.app.NotificationManager::class.java).cancel("focus", 1)
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    companion object {
        private val lock = Any()
        const val WORK = "taskline-focus-completion"
    }
}

class FocusCompletionWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("session") ?: return Result.failure()
        return try {
            val store = FocusStore(applicationContext)
            val state = store.read()
            if (state.sessionId == id && state.isActive && state.remainingSeconds > 0) return Result.retry()
            store.complete(id)
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("TaskLineFocus", "Unable to finish focus session", error)
            Result.retry()
        }
    }
}
