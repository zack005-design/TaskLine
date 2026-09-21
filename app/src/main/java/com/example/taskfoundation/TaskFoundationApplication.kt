package com.example.taskfoundation

import android.app.Application

class TaskFoundationApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(this)
    }
}
