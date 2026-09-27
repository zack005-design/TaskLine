package com.example.taskfoundation

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.taskfoundation.ui.TaskLineTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskfoundation.ui.AppViewModelFactory
import com.example.taskfoundation.ui.TaskLineScreen

class MainActivity : ComponentActivity() {
    private val requestedTask = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Liquid Glass: enable system background blur on API 31+ (Android 12).
        // This makes glass surfaces blur the wallpaper/content behind the window,
        // matching Apple's Liquid Glass depth and translucency effect.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(20)
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
        if (savedInstanceState == null) requestedTask.value = intent.getLongExtra("taskId", -1).takeIf { it > 0 }
        val factory = AppViewModelFactory((application as TaskFoundationApplication).container)
        setContent {
            TaskLineTheme {
                TaskLineScreen(viewModel(factory = factory), viewModel(factory = factory), viewModel(factory = factory),
                    viewModel(factory = factory), requestedTask.value, { requestedTask.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTask.value = intent.getLongExtra("taskId", -1).takeIf { it > 0 }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try { (application as TaskFoundationApplication).container.reminders.reconcile() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Log.e("TaskReminders", "Unable to refresh reminders", error) }
        }
    }
}
