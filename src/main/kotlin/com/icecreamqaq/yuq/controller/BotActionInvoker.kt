package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ActionContext
import com.IceCreamQAQ.Yu.controller.DefaultActionInvoker
import com.icecreamqaq.yuq.annotation.TaskLimit
import com.icecreamqaq.yuq.entity.MessageAt
import com.icecreamqaq.yuq.error.SkipMe
import com.icecreamqaq.yuq.message.At
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.Message.Companion.toMessageByRainCode
import com.icecreamqaq.yuq.internalBot
import com.icecreamqaq.yuq.yuq
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Method

interface TaskLimitHandler {

    fun auth(context: BotActionContext): Boolean
    fun start(context: BotActionContext)
    fun end(context: BotActionContext)

    abstract class BaseHandler : TaskLimitHandler {
        //        lateinit var messageBuilder: MessageBuilder
        lateinit var cdMessage: Message
        var cd: Long = 0

        fun makeMessage(msgStr: String) {
            cdMessage = internalBot.rainCode.run {
                if (enable)
                    if (msgStr.startsWith(prefix)) msgStr.substring(prefix.length).toMessageByRainCode()
                    else msgStr.toMessage()
                else msgStr.toMessage()
            }
        }

        abstract fun doAuth(context: BotActionContext): Boolean
        abstract fun doEnd(context: BotActionContext)

        override fun auth(context: BotActionContext): Boolean {
            if (!doAuth(context)) {
                context.reMessage = cdMessage
                return false
            }
            return true
        }

        override fun end(context: BotActionContext) {
            if (cd > 0)
                GlobalScope.launch {
                    delay(cd)
                    doEnd(context)
                }
            else doEnd(context)
        }


    }


    class All : BaseHandler() {

        var auth = true

        override fun start(context: BotActionContext) {
            auth = false
        }

        override fun doAuth(context: BotActionContext) = auth
        override fun doEnd(context: BotActionContext) {
            auth = true
        }

    }

    class Source : BaseHandler() {
        private var auth = HashSet<String>()

        override fun doAuth(context: BotActionContext) = !auth.contains(context.source.guid)

        override fun start(context: BotActionContext) {
            auth.add(context.source.guid)
        }

        override fun doEnd(context: BotActionContext) {
            auth.remove(context.source.guid)
        }
    }

    class Sender : BaseHandler() {
        private var auth = HashSet<String>()

        override fun doAuth(context: BotActionContext) = !auth.contains(context.sender.guid)
        override fun start(context: BotActionContext) {
            auth.add(context.sender.guid)
        }

        override fun doEnd(context: BotActionContext) {
            auth.remove(context.sender.guid)
        }
    }

    object Factory {

        fun build(taskLimit: TaskLimit?): TaskLimitHandler? {
            if (taskLimit == null) return null
            val handler = when (taskLimit.type) {
                TaskLimit.TaskLimitSource.ALL -> All()
                TaskLimit.TaskLimitSource.SOURCE -> Source()
                TaskLimit.TaskLimitSource.SENDER -> Sender()
            }
            handler.cd = taskLimit.value
            handler.makeMessage(taskLimit.coldDownTip)
            return handler
        }

    }

}

open class BotActionInvoker(level: Int, method: Method, instance: Any) : DefaultActionInvoker(level, method, instance) {

    var decodeRainCode: Boolean = false
    var at: Boolean = false
    var atNewLine: Boolean = false
    var reply: Boolean = false
    var nextContext: NextActionContext? = null
    var mastAtBot = false
    var recall: Long? = null
    var forceMatch = false

    var taskLimitHandler: TaskLimitHandler? =
        TaskLimitHandler.Factory.build(method.getAnnotation(TaskLimit::class.java))

//    override val invoker: MethodInvoker = BotReflectMethodInvoker(method, instance, level)

    override fun createMethodInvoker(method: Method, instance: Any) = BotReflectMethodInvoker(method, instance, level)

    suspend fun superInvoke(path: String, context: ActionContext): Boolean {
        val cps = context.path.size
        val nextPath = when {
            level > cps -> return false
            level == cps -> ""
            else -> context.path[level]
        }
        val nor = noMatch[path]
        if (nor != null) return nor.invoke(nextPath, context)
        for (matchItem in needMath) {
            val m = matchItem.p.matcher(path)
            if (m.find()) {
                if (matchItem.needSave) {
                    for ((i, name) in matchItem.matchNames!!.withIndex()) {
                        context[name] = m.group(i + 1)
                    }
                }
                if (matchItem.invoke(nextPath, context)) return true
            }
        }
        return false
    }

    override suspend fun invoke(path: String, context: ActionContext): Boolean {
        if (context !is BotActionContext) return false
        if (superInvoke(path, context)) return true
//         reMessage: Message?
        if (forceMatch) {
            if (context.path.size + 1 > level) return false
        }
        if (mastAtBot) {
            if (((context.message.body[0] as? At)?.user ?: -1) != yuq.botId) return false
        }
        try {
            context.actionInvoker = this
            val reMessage = if (taskLimitHandler?.auth(context) != false) {
                taskLimitHandler?.start(context)
                for (before in befores)
                    before.invoke(context)?.let { o -> context[o::class.java.simpleName.toLowerCaseFirstOne()] = o }

                var re = invoker.invoke(context)
                if (nextContext != null && context.nextContext == null) context.nextContext = nextContext
                if (decodeRainCode) if (re is String) re = re.toMessageByRainCode()
                val reMessage = context.onSuccess(re ?: return true) as? Message ?: return true

                for (after in afters)
                    after.invoke(context)?.let { o -> context[o::class.java.simpleName.toLowerCaseFirstOne()] = o }


                reMessage
            } else context.reMessage!!

            recall?.let { reMessage.recallDelay = it }
            if (reply) reMessage.reply = context.message.source
            if (at) reMessage.at = MessageAt(context.sender.id, atNewLine)

            taskLimitHandler?.end(context)
            return true
        } catch (e: Exception) {
            val r = context.onError(e) ?: return true
            if (r is SkipMe) {
                context.actionInvoker = null
                taskLimitHandler?.end(context)
                return false
            }
//            throw r

//            val er= context.onError(e) ?: return true
            context["exception"] = r
            try {
                for (catch in catchs) {
                    val o = catch.invoke(context, r)
                    if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
                }
                taskLimitHandler?.end(context)
                throw r
            } catch (ee: Exception) {
                taskLimitHandler?.end(context)
                throw context.onError(ee) ?: return true
            }
        }


    }

    private fun String.toLowerCaseFirstOne(): String {
        return if (Character.isLowerCase(this[0])) this
        else (StringBuilder()).append(Character.toLowerCase(this[0])).append(this.substring(1)).toString();
    }
}
