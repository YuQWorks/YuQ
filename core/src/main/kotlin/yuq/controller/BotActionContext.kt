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

    val cursor = MatcherItem(message.body)
    internal val matcherItem: MatcherItem get() = cursor

    private val saved = HashMap<String, Any?>()
    private object AbsentParameter

    override var result: Any? = null
    override val user: IUser?
        get() = sender as? IUser

    override var runtimeError: Throwable? = null
    var actionInvoker: BotActionInvoker? = null
    internal var attemptedActions = HashSet<BotActionInvoker>()

    class Checkpoint internal constructor(
        internal val position: Pair<Int, Int>,
        internal val values: Map<String, Any?>,
        internal val result: Any?,
        internal val runtimeError: Throwable?,
        internal val actionInvoker: BotActionInvoker?
    )

    fun checkpoint(): Checkpoint = Checkpoint(cursor.mark(), saveValues(), result, runtimeError, actionInvoker)

    fun rollback(checkpoint: Checkpoint) {
        cursor.reMark(checkpoint.position)
        restoreValues(checkpoint.values)
        result = checkpoint.result
        runtimeError = checkpoint.runtimeError
        actionInvoker = checkpoint.actionInvoker
    }

    fun contains(name: String): Boolean = saved.containsKey(name)

    /** An omitted route parameter must not fall through to contact or service injection. */
    fun isAbsent(name: String): Boolean = saved[name] === AbsentParameter

    internal fun markAbsent(name: String) { saved[name] = AbsentParameter }

    internal fun saveValues(): Map<String, Any?> = HashMap(saved)

    internal fun restoreValues(values: Map<String, Any?>) {
        saved.clear()
        saved.putAll(values)
    }


    override fun get(name: String): Any? {
        return saved[name].takeUnless { it === AbsentParameter }
    }

    override fun remove(name: String): Any? {
        return saved.remove(name).takeUnless { it === AbsentParameter }
    }

    override fun set(name: String, obj: Any?) {
        saved[name] = obj
    }
}
