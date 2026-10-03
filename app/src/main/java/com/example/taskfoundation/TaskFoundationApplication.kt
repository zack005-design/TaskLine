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
            container.database.libraryDao().observe().retryWhen { cause, _ ->
                Log.e("LibraryWidget", "Library observation failed", cause)
                com.example.taskfoundation.widget.LibraryWidget.showError(this@TaskFoundationApplication)
                delay(5000); true
            }.collect {
                try { com.example.taskfoundation.widget.LibraryWidget.refresh(this@TaskFoundationApplication) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { Log.e("LibraryWidget", "Refresh failed", e); com.example.taskfoundation.widget.LibraryWidget.showError(this@TaskFoundationApplication) }
            }
        }
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
