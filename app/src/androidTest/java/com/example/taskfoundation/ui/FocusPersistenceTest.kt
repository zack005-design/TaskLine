package com.example.taskfoundation.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.focus.FocusStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class FocusPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "focus-test-${java.util.UUID.randomUUID()}"
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val store = FocusStore(context, name)
    @After fun clearTestData() { prefs.edit().clear().commit() }

    @Test fun restoredSessionKeepsDeadlineAndStaleCompletionCannotFinishReplacement() {
        val task = Task(id = 1, title = "Focus persistence test", createdAt = 0, updatedAt = 0)
        val first = store.start(task, 1)
        val restored = FocusStore(context, name).read()
        assertEquals(first.sessionId, restored.sessionId)
        assertEquals(first.endsAt, restored.endsAt)
        assertTrue(restored.isActive)
        val second = store.start(task.copy(id = 2), 1)
        assertNull(store.schedule(first))
        prefs.edit().putLong("end", System.currentTimeMillis() - 1000).commit()
        store.complete(first.sessionId!!)
        assertTrue(store.read().isActive)
        store.complete(second.sessionId!!)
        assertFalse(store.read().isActive)
        assertEquals(0, store.read().remainingSeconds)
        store.complete(second.sessionId)
        assertEquals(second.sessionId, store.read().sessionId)
    }
}
