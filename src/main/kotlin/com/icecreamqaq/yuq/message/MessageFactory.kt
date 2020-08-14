package com.icecreamqaq.yuq.message

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
interface MessageFactory {

    fun newMessage():Message
    fun newGroup(group:Long):Message
    fun newPrivate(qq:Long):Message
    fun newTemp(group:Long,qq:Long):Message

}

class MessageFactoryImpl : MessageFactory {
    override fun newMessage(): Message {
        return Message()
    }

    override fun newGroup(group: Long): Message {
        val message = newMessage()
        message.group = group
        return message
    }

    override fun newPrivate(qq: Long): Message {
        val message = Message()
        message.qq = qq
        return message
    }

    override fun newTemp(group: Long, qq: Long): Message {
        val message = Message()
        message.temp = true
        message.qq = qq
        message.group = group
        return message
    }
}