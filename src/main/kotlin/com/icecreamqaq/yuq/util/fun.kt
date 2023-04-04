package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.event.events.Event
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.message.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

lateinit var yuq: YuQ
lateinit var mif: MessageItemFactory
lateinit var web: Web

internal lateinit var botService: BotService

lateinit var eventBus: EventBus
fun Event.post() = eventBus.post(this)
operator fun Event.invoke() = this.post()

@JvmName("postEvent")
inline fun <R> post(event: Event, success: () -> R? = { null }, cancel: () -> R? = { null }) =
    if (event()) cancel() else success()

inline fun <R> Event.post(success: () -> R? = { null }, cancel: () -> R? = { null }) = post(this, success, cancel)
inline operator fun <R> Event.invoke(success: () -> R? = { null }, cancel: () -> R? = { null }) =
    post(this, success, cancel)

inline operator fun Event.invoke(cancel: () -> Unit) {
    if (this.post()) cancel()
}

operator fun Event.invoke(error: Throwable) {
    if (this.post()) throw error
}

suspend inline fun asyncDelay(time: Long, crossinline body: () -> Unit) {
    coroutineScope {
        launch {
            delay(time)
            body()
        }
    }
}