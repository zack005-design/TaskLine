package com.example.taskfoundation.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.example.taskfoundation.domain.model.Task
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AddTaskSheetStateTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draftTitleAndPendingSubtasksSurviveStateRestoration() {
        val restoration = StateRestorationTester(compose)
        var saved: Pair<Task, List<String>>? = null
        restoration.setContent {
            TaskLineTheme {
                AddTaskSheet(null, emptyList(), null, false, null, null, {}, { task, titles -> saved = task to titles })
            }
        }
        compose.onNodeWithContentDescription("Title").performTextInput("Unsaved draft")
        compose.onNodeWithText("+ Add subtask").performScrollTo().performClick()
        compose.onNodeWithText("Subtask 1").performScrollTo().performTextInput("Remember this child")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Save task").performClick()
        compose.runOnIdle {
            assertEquals("Unsaved draft", saved!!.first.title)
            assertEquals(listOf("Remember this child"), saved!!.second)
        }
    }
}
