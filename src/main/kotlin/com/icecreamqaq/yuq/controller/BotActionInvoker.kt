package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.NewActionContext
import com.IceCreamQAQ.Yu.controller.router.NewActionInvoker
import com.IceCreamQAQ.Yu.controller.router.NewMethodInvoker
import com.icecreamqaq.yuq.entity.MessageAt
import com.icecreamqaq.yuq.error.SkipMe
import com.icecreamqaq.yuq.message.Message
import java.lang.reflect.Method

open class BotActionInvoker(level: Int, method: Method, instance: Any) : NewActionInvoker(level, method, instance) {

    var at: Boolean = false
    var atNewLine: Boolean = false
    var reply: Boolean = false
    var nextContext: NextActionContext? = null

    override val invoker: NewMethodInvoker = BotReflectMethodInvoker(method, instance, level)


    fun superInvoke(path: String, context: NewActionContext): Boolean {
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

    override fun invoke(path: String, context: NewActionContext): Boolean {
        if (context !is BotActionContext) return false
        if (superInvoke(path, context)) return true

//         reMessage: Message?
        try {
            context.actionInvoker = this
            for (before in globalBefores) {
                val o = before.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
            for (before in befores) {
                val o = before.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
            val re = invoker.invoke(context)
            if (nextContext != null && context.nextContext == null) context.nextContext = nextContext
            val reMessage = context.onSuccess(re ?: return true) as? Message ?: return true
            for (after in globalAfters) {
                val o = after.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
            for (after in afters) {
                val o = after.invoke(context)
                if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
            }
            if (reply) reMessage.reply = context.message.source
            if (at) reMessage.at = MessageAt(context.sender.id, atNewLine)
            return true
        } catch (e: Exception) {
            val r = context.onError(e) ?: return true
            if (r is SkipMe) {
                context.actionInvoker = null
                return false
            }
//            throw r

//            val er= context.onError(e) ?: return true
            context["exception"] = r
            try {
                for (catch in globalCatchs) {
                    val o = catch.invoke(context, r)
                    if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
                }
                for (catch in catchs) {
                    val o = catch.invoke(context, r)
                    if (o != null) context[o::class.java.simpleName.toLowerCaseFirstOne()] = o
                }
                throw r
            } catch (ee: Exception) {
                throw context.onError(ee) ?: return true
            }
        }


    }

    private fun String.toLowerCaseFirstOne(): String {
        return if (Character.isLowerCase(this[0])) this
        else (StringBuilder()).append(Character.toLowerCase(this[0])).append(this.substring(1)).toString();
    }
}
