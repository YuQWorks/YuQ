package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactory
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageItemFactory

lateinit var yuq:YuQ

lateinit var mf: MessageFactory
lateinit var mif: MessageItemFactory

operator fun String.minus(messageItem: MessageItem) = mif.text(this) + messageItem
fun String.toText() = mif.text(this)
fun String.toMessage() = this.toText().toMessage()

fun Message.send() = yuq.sendMessage(this)