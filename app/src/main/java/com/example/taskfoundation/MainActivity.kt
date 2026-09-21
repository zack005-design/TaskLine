package com.example.taskfoundation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskfoundation.ui.AppViewModelFactory
import com.example.taskfoundation.ui.projects.ProjectsViewModel
import com.example.taskfoundation.ui.tasks.TasksViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = AppViewModelFactory((application as TaskFoundationApplication).container)
        setContent {
            MaterialTheme {
                FoundationScreen(factory)
            }
        }
    }
}

@Composable
private fun FoundationScreen(factory: AppViewModelFactory) {
    val tasksViewModel: TasksViewModel = viewModel(factory = factory)
    val projectsViewModel: ProjectsViewModel = viewModel(factory = factory)
    val tasksState by tasksViewModel.uiState.collectAsStateWithLifecycle()
    val projectsState by projectsViewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tasks", style = MaterialTheme.typography.headlineMedium)
            Text("${tasksState.tasks.size} tasks · ${projectsState.projects.size} projects")
            tasksState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            projectsState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
