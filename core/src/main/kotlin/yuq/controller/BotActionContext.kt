package yuq.controller

import rain.api.permission.IUser
import rain.controller.ActionContext
import yuq.Bot
import yuq.contact.Contact
import yuq.message.Message

class BotActionContext(
    val bot: Bot,
    val channel: MessageChannel,
    val sender: Contact,
    val source: Contact,
    val message: Message
) : ActionContext {

    override val user: IUser
        get() = sender

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
