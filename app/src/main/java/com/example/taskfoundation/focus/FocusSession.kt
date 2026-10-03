package com.example.taskfoundation.focus

data class FocusState(
    val isActive: Boolean = false,
    val taskId: Long? = null,
    val taskTitle: String = "",
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val sessionId: String? = null,
    val endsAt: Long = 0,
    val error: String? = null,
    val isPaused: Boolean = false,
)

/** Derive remaining time from a deadline rather than counting delayed coroutine ticks. */
fun remainingSeconds(endsAt: Long, now: Long, totalSeconds: Int): Int =
    ((endsAt - now).coerceAtLeast(0) / 1000.0).let { kotlin.math.ceil(it).toInt() }.coerceIn(0, totalSeconds)
