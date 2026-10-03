package com.example.taskfoundation.widget

import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.os.Bundle
import android.widget.RemoteViews
import com.example.taskfoundation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A separate home-screen surface for habits, milestones and notes. */
class LibraryWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        onUpdate(context, manager, intArrayOf(id))
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { withTimeout(8000) { refresh(context) } }
            catch (e: Exception) { showError(context); android.util.Log.e("LibraryWidget", "Refresh failed", e) }
            finally { pending.finish() }
        }
    }
    companion object {
        private val lock = Mutex()
        private fun views(context: Context) = RemoteViews(context.packageName, R.layout.library_widget).apply {
            setTextViewText(R.id.widget_heading, "Daily rituals")
            val intent = Intent(context, MainActivity::class.java).putExtra("openLibrary", true)
            setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 8001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        suspend fun refresh(context: Context) = lock.withLock {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LibraryWidget::class.java))
            if (ids.isEmpty()) return@withLock
            val items = (context.applicationContext as TaskFoundationApplication).container.database.libraryDao().all()
                .filter { it.kind in listOf("Habit", "Countdown", "Note") }
            val today = LocalDate.now()
            val rows = items.sortedWith(compareBy({ if (it.kind == "Habit") 0 else if (it.kind == "Countdown") 1 else 2 }, { it.createdAt }))
                .take(3).map { item ->
                    val data = JSONObject(item.payload)
                    val detail = when (item.kind) {
                        "Habit" -> "${data.getJSONObject("days").optInt(today.toString())} / ${data.getInt("goal")} today"
                        "Countdown" -> { val days = ChronoUnit.DAYS.between(today, LocalDate.parse(data.getString("date")))
                            when { days > 0 -> "$days days to go"; days == 0L -> "Today"; else -> "${-days} days ago" } }
                        else -> data.getString("text").replace('\n', ' ')
                    }
                    "${item.title}\n$detail"
                }
            ids.forEach { widgetId ->
                val views = views(context)
                views.setTextViewText(R.id.widget_summary, "Habits · milestones · notes")
                val height = manager.getAppWidgetOptions(widgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 340)
                val scale = context.resources.configuration.fontScale
                val capacity = ((height - 48 - 86 * scale) / (24 + 38 * scale)).toInt().coerceIn(1, 3)
                listOf(R.id.widget_task_one, R.id.widget_task_two, R.id.widget_task_three).forEachIndexed { index, id ->
                    views.setTextViewText(id, rows.getOrNull(index) ?: "Add your first habit in Library")
                    views.setViewVisibility(id, if (index < capacity && (index == 0 || index < rows.size)) android.view.View.VISIBLE else android.view.View.GONE)
                }
                manager.updateAppWidget(widgetId, views)
            }
        }
        fun showError(context: Context) {
            val views = views(context)
            views.setTextViewText(R.id.widget_summary, "Open Library to reload")
            AppWidgetManager.getInstance(context).updateAppWidget(ComponentName(context, LibraryWidget::class.java), views)
        }
    }
}
