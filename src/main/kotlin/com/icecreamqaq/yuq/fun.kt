package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.event.events.Event
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.message.*

lateinit var yuq: YuQ

//@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
//lateinit var mf: MessageFactory
lateinit var mif: MessageItemFactory
lateinit var web: Web

internal lateinit var internalBot: YuQInternalBotImpl

lateinit var eventBus: EventBus
fun Event.post() = eventBus.post(this)
operator fun Event.invoke() = this.post()

@JvmName("postEvent")
inline fun <R> post(event: Event, success: () -> R? = { null }, cancel: () -> R? = { null }) = if (event()) cancel() else success()
inline fun <R> Event.post(success: () -> R? = { null }, cancel: () -> R? = { null }) = post(this, success, cancel)
inline operator fun <R> Event.invoke(success: () -> R? = { null }, cancel: () -> R? = { null }) = post(this, success, cancel)


//operator fun String.minus(messageItem: MessageItem) = mif.text(this) + messageItem


//@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
//fun Message.send() = yuq.sendMessage(this)
//fun Message.firstString() = com.icecreamqaq.yuq.message.Message.firstString(this)