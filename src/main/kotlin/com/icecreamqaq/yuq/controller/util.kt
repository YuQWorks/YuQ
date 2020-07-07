package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.annotation.Before
import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactory
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageItemFactory
import com.icecreamqaq.yuq.send
import javax.inject.Inject
import javax.inject.Named

abstract class QQController {

    @Inject
    lateinit var yuq: YuQ

    @Inject
    lateinit var mf: MessageFactory

    @Inject
    lateinit var mif: MessageItemFactory


    private val qqControllerUtilActionContextLocal: ThreadLocal<BotActionContext> = ThreadLocal()

    @Before
    fun qqControllerUtilBefore(@Named("actionContext") actionContext: BotActionContext) {
        qqControllerUtilActionContextLocal.set(actionContext)
    }

    fun reply(msg: String) = ((qqControllerUtilActionContextLocal.get()?.message?.newMessage()
            ?: error("当前线程没有 Action 上下文。")) + msg).send()

    fun reply(msg: MessageItem) = ((qqControllerUtilActionContextLocal.get()?.message?.newMessage()
            ?: error("当前线程没有 Action 上下文。")) + msg).send()

    fun reply(msg: Message) = ((qqControllerUtilActionContextLocal.get()?.message?.newMessage()
            ?: error("当前线程没有 Action 上下文。")) + msg).send()

}