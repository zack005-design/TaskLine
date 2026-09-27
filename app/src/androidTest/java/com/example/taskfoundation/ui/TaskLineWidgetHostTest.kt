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
