package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactory
import com.icecreamqaq.yuq.message.MessageItemFactory
import com.icecreamqaq.yuq.message.Text

lateinit var yuq:YuQ
@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
lateinit var mf: MessageFactory
lateinit var mif: MessageItemFactory

//operator fun String.minus(messageItem: MessageItem) = mif.text(this) + messageItem
fun String.toText() = mif.text(this)
fun String.toMessage() = this.toText().toMessage()

@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
fun Message.send() = yuq.sendMessage(this)
fun Message.firstString(): String {
    for (item in body) {
        if (item is Text) return item.text
    }
    error("消息不包含任何一个文本串。")
}



fun Member.toFriend(): Friend? = yuq.friends[id]