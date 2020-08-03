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