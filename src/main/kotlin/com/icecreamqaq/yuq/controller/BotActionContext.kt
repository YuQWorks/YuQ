package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ActionContext
import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.message.Message

class BotActionContext(
    val bot: Bot,
    val channel: MessageChannel,
    val sender: Contact,
    val source: Contact,
    val message: Message
) : ActionContext {
    private val saved = HashMap<String, Any?>()

    override var result: Any? = null
    override var runtimeError: Throwable? = null


    var actionInvoker: BotActionInvoker? = null


    override fun get(name: String): Any? {
        return saved[name]
    }

    override fun remove(name: String): Any? {
        return saved.remove(name)
    }

    override fun set(name: String, obj: Any?) {
        saved[name] = obj
    }
}
