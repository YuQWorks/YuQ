package com.icecreamqaq.yuq

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend inline fun asyncDelay(time: Long, crossinline body: () -> Unit) {
    coroutineScope {
        launch {
            delay(time)
            body()
        }
    }
}