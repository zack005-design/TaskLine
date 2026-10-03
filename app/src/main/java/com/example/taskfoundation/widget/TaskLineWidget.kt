package com.example.taskfoundation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.taskfoundation.MainActivity
import com.example.taskfoundation.R
import com.example.taskfoundation.TaskFoundationApplication
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.time.CalendarDates
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun upcomingTasks(tasks: List<Task>): List<Task> = tasks.filterNot { it.isCompleted }
    .sortedWith(compareBy<Task> { it.dueDateTime ?: Long.MAX_VALUE }
        .thenByDescending { it.priority.ordinal }.thenBy { it.id }).take(3)

class TaskLineWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8000) { refresh(context) }
            } catch (error: Exception) {
                Log.e("TaskLineWidget", "Unable to refresh tasks", error)
                showError(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val refreshLock = Mutex()

        suspend fun refresh(context: Context) = refreshLock.withLock {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskLineWidget::class.java))
            if (ids.isEmpty()) return@withLock
            val tasks = (context.applicationContext as TaskFoundationApplication)
                .container.taskRepository.observeTasks().first()
            val active = tasks.count { !it.isCompleted }
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            val upcoming = upcomingTasks(tasks)
            val rows = upcoming.map { task ->
                val date = task.dueDateTime?.let {
                    CalendarDates.decode(it).format(formatter)
                } ?: "No due date"
                "${task.title}\n$date"
            }
            ids.forEach { widgetId ->
                val views = baseViews(context)
                views.setTextViewText(R.id.widget_summary, "$active active tasks")
                val height = manager.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 340)
                val scale = context.resources.configuration.fontScale
                // Leave room for the header and padding; larger text shows fewer complete rows.
                val capacity = ((height - 48 - 86 * scale) / (24 + 38 * scale)).toInt().coerceIn(1, 3)
                listOf(R.id.widget_task_one, R.id.widget_task_two, R.id.widget_task_three).forEachIndexed { index, id ->
                    upcoming.getOrNull(index)?.let { task ->
                        val intent = Intent(context, MainActivity::class.java).putExtra("taskId", task.id)
                            .setData(android.net.Uri.parse("taskline://task/${task.id}"))
                        views.setOnClickPendingIntent(id, PendingIntent.getActivity(context, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    }
                    views.setTextViewText(id, rows.getOrNull(index) ?: if (index == 0) "All clear. Add your next task in TaskLine." else "")
                    views.setViewVisibility(id, if (index < capacity && (index == 0 || index < rows.size)) android.view.View.VISIBLE else android.view.View.GONE)
                }
                manager.updateAppWidget(widgetId, views)
            }
        }

        fun showError(context: Context) {
            val views = baseViews(context)
            views.setTextViewText(R.id.widget_summary, "Open TaskLine to reload your tasks")
            AppWidgetManager.getInstance(context).updateAppWidget(ComponentName(context, TaskLineWidget::class.java), views)
        }

        private fun baseViews(context: Context): RemoteViews {
            val intent = Intent(context, MainActivity::class.java)
            val open = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteViews(context.packageName, R.layout.taskline_widget).apply {
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }
    }
}
