package com.example.taskfoundation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.taskfoundation.domain.model.Task
import com.example.taskfoundation.domain.model.TaskPriority
import com.example.taskfoundation.domain.model.TaskStatus

@Preview(name = "TaskLine · Light", showBackground = true)
@Preview(name = "TaskLine · Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "TaskLine · Large text", fontScale = 1.5f)
@Composable
private fun TaskLinePreview() {
    val task = Task(title = "Bring the next idea to life", description = "A small step toward something meaningful.",
        priority = TaskPriority.HIGH, status = TaskStatus.IN_PROGRESS, progress = 40, createdAt = 0, updatedAt = 0)
    TaskLineTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OverviewCard(listOf(task))
                TaskCard(task, "Personal projects", "Due 28 Sep", false, {}, {}, {}, {})
            }
        }
    }
}
