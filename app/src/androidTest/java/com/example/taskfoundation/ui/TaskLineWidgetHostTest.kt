package com.example.taskfoundation.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.example.taskfoundation.R
import com.example.taskfoundation.TaskFoundationApplication
import com.example.taskfoundation.widget.TaskLineWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TaskLineWidgetHostTest {
    @Test fun ritualsWidgetBindsWithItsOwnLayoutAndOpensLibrary() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 7342)
        val id = host.allocateAppWidgetId()
        instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        try {
            assertTrue(manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, com.example.taskfoundation.widget.LibraryWidget::class.java)))
            runBlocking { com.example.taskfoundation.widget.LibraryWidget.refresh(context) }
            instrumentation.runOnMainSync {
                val view = host.createView(context, id, manager.getAppWidgetInfo(id))
                assertEquals("Daily rituals", view.findViewById<TextView>(R.id.widget_heading).text.toString())
                assertEquals("Habits · milestones · notes", view.findViewById<TextView>(R.id.widget_summary).text.toString())
                assertTrue(view.findViewById<android.view.View>(R.id.widget_root).hasOnClickListeners())
            }
        } finally { host.deleteAppWidgetId(id); instrumentation.uiAutomation.dropShellPermissionIdentity() }
    }
    @Test fun threeTaskRowsFitTheMinimumWidgetSize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val views = android.widget.RemoteViews(context.packageName, R.layout.taskline_widget)
            views.setTextViewText(R.id.widget_summary, "4 active tasks")
            views.setTextViewText(R.id.widget_task_one, "Refine the new homepage\nToday · 10:00")
            views.setTextViewText(R.id.widget_task_two, "A little time outside\nToday · 18:00")
            views.setTextViewText(R.id.widget_task_three, "Collect ideas for next week\nTomorrow")
            val view = views.apply(context, android.widget.FrameLayout(context))
            val density = context.resources.displayMetrics.density
            val width = (300 * density).toInt()
            val height = (340 * density).toInt()
            view.measure(android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY))
            view.layout(0, 0, width, height)
            assertTrue("The final task must fit above the bottom padding", view.findViewById<TextView>(R.id.widget_task_three).bottom <= height - view.paddingBottom)
            val finalRow = view.findViewById<TextView>(R.id.widget_task_three)
            assertTrue("The final date line must not be clipped",
                finalRow.layout.getLineBottom(finalRow.layout.lineCount - 1) <= finalRow.height - finalRow.compoundPaddingTop - finalRow.compoundPaddingBottom)
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            java.io.File(context.getExternalFilesDir(null), "redesign-widget.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    @Test fun widgetBindsAndRendersPersistedTaskCount() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 7341)
        val id = host.allocateAppWidgetId()
        instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
        try {
            assertTrue(manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, TaskLineWidget::class.java)))
            runBlocking { TaskLineWidget.refresh(context) }
            val active = runBlocking {
                (context.applicationContext as TaskFoundationApplication).container.taskRepository.observeTasks().first()
                    .count { !it.isCompleted }
            }
            instrumentation.runOnMainSync {
                val view = host.createView(context, id, manager.getAppWidgetInfo(id))
                val summary = view.findViewById<TextView>(R.id.widget_summary)
                assertNotNull("RemoteViews must inflate successfully", summary)
                assertEquals("$active active tasks", summary.text.toString())
                assertTrue(view.findViewById<android.view.View>(R.id.widget_root).hasOnClickListeners())
            }
        } finally {
            host.deleteAppWidgetId(id)
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
