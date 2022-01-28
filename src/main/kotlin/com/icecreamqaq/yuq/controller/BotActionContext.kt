package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ActionContext
import com.IceCreamQAQ.Yu.entity.DoNone
import com.IceCreamQAQ.Yu.entity.Result
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.error.MessageThrowable
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.Message.Companion.toMessageByRainCode
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageLineQ
import com.icecreamqaq.yuq.internalBot
import com.icecreamqaq.yuq.message.MessageItemChain
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class BotActionContext(
    val source: Contact,
    val sender: Contact,
    val message: Message,
    var session: ContextSession,
    var groupSession: ContextSession? = null,
    /***
     * 0 -> 群消息
     * 1 -> 好友消息
     * 2 -> 临时会话
     */
    var messageType: Int,
    override var path: Array<String> = message.toPath().toTypedArray(),
) : ActionContext {


    var reMessage: Message? = null

    var nextContext: NextActionContext? = null

    var actionInvoker: BotActionInvoker? = null

    private val saved = HashMap<String, Any?>()

    @Deprecated("功能已经转移到 Message 的 recallDelay 字段。")
    var recall: Long? = null

    init {
        saved["actionContext"] = this
        saved["context"] = this

        saved["session"] = session
        saved["contextSession"] = session

        saved["path"] = path

        saved["sourceMessage"] = message.sourceMessage

        saved["reply"] = message.reply

        saved["message"] = message
    }


    override fun get(name: String): Any? {
        return saved[name] ?: session[name]
    }

    override fun set(name: String, obj: Any) {
        saved[name] = obj
    }

    override suspend fun onError(e: Throwable) =
        when (e) {
            is DoNone -> null
            is MessageThrowable -> {
                this.reMessage = buildResult(e.c)
                null
            }
            is Result -> {
                this.reMessage = buildResult(e)
                null
            }
            is NextActionContext -> {
                this.nextContext = e
                null
            }
            else -> e
        }

    override suspend fun onSuccess(result: Any?): Any? {
        if (result == null) return null
        this.reMessage = buildResult(result) ?: return null
        saved["reMessage"] = reMessage
        return reMessage
    }

    private suspend fun buildResult(obj: Any): Message? {
        return when (obj) {
            is String ->
                internalBot.rainCode.run {
                    if (enable)
                        if (obj.startsWith(prefix)) obj.substring(prefix.length).toMessageByRainCode()
                        else obj.toMessage()
                    else obj.toMessage()
                }
            is Array<*> -> {
                for (any in obj) {
                    when (any) {
                        is Int -> coroutineScope { delay(any.toLong()) }
                        is Long -> coroutineScope { delay(any) }
                        else -> any?.let { buildResult(it)?.let { message -> source.sendMessage(message) } }
                    }
                }
                null
            }
            is MessageItem -> {
                val message = Message()
                val mb = message.body
                mb.append(obj)
                message
            }
            is MessageItemChain -> obj.toMessage()
            is Message -> obj
            is MessageLineQ -> obj.message
            else -> buildResult(obj.toString())
        }
    }
}
