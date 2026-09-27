package com.example.taskfoundation.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskfoundation.domain.model.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val store = FocusStore(application)
    private val mutableState = MutableStateFlow(FocusState())
    val state = mutableState.asStateFlow()
    private var timer: Job? = null

    init { restore() }

    private fun restore() {
        timer = viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { store.read() }
                mutableState.value = saved
                if (saved.isActive) {
                    withContext(Dispatchers.IO) { store.schedule(saved).result.get() }
                    tick()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = "Unable to restore focus session. Try again.") } }
        }
    }

    fun start(task: Task, minutes: Int = 25) {
        timer?.cancel()
        timer = viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { store.start(task, minutes) }
                mutableState.value = saved
                withContext(Dispatchers.IO) { store.schedule(saved).result.get() }
                tick()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = "Unable to schedule focus. Stop and try again.") } }
        }
    }

    private suspend fun tick() {
        while (true) {
            val value = withContext(Dispatchers.IO) {
                val current = store.read()
                if (current.isActive && current.remainingSeconds == 0) store.complete(current.sessionId!!)
                store.read()
            }
            mutableState.value = value
            if (!value.isActive) break
            delay(250)
        }
    }

    fun stop() {
        timer?.cancel()
        timer = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { store.stop().result.get() }
                mutableState.value = FocusState()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = "Unable to stop focus. Please retry.") } }
        }
    }
}
