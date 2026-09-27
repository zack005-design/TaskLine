package com.example.taskfoundation

import android.app.Application
import android.util.Log
import com.example.taskfoundation.widget.TaskLineWidget
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.retryWhen

class TaskFoundationApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try { com.example.taskfoundation.reminders.SmartNudge.schedule(this@TaskFoundationApplication).result.get() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Log.e("TaskLineNudge", "Unable to schedule weekly progress", error) }
        }
        applicationScope.launch {
            container.taskRepository.observeTasks().retryWhen { cause, _ ->
                Log.e("TaskLineWidget", "Task observation failed; retrying", cause)
                TaskLineWidget.showError(this@TaskFoundationApplication)
                delay(5000)
                true
            }.collect {
                try {
                    container.reminders.reconcile()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("TaskReminders", "Reminder scheduling failed", error)
                }
                try {
                    TaskLineWidget.refresh(this@TaskFoundationApplication)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("TaskLineWidget", "Widget refresh failed", error)
                    TaskLineWidget.showError(this@TaskFoundationApplication)
                }
            }
        }
    }

    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }
}
