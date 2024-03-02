package com.icecreamqaq.yuq.controller

import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.contact.Contact
import com.icecreamqaq.yuq.message.Message
import rain.controller.ActionContext

class BotActionContext(
    val bot: Bot,
    val channel: MessageChannel,
    val sender: Contact,
    val source: Contact,
    val message: Message
) : ActionContext {

    internal val matcherItem = MatcherItem(message.body)

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
