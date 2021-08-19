package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.DefaultControllerLoaderImpl
import com.icecreamqaq.yuq.annotation.NextContext
import com.icecreamqaq.yuq.annotation.QMsg
import com.icecreamqaq.yuq.annotation.RainCodeString
import java.lang.reflect.Method

open class BotControllerLoader : DefaultControllerLoaderImpl() {

    override val separationCharacter: Array<String> = arrayOf(" ", "/")

    override fun createMethodInvoker(obj: Any, method: Method) = BotReflectMethodInvoker(method, obj)

    override fun createActionInvoker(level: Int, actionMethod: Method, instance: Any): BotActionInvoker {
        val ai = BotActionInvoker(level, actionMethod, instance)
        ai.nextContext =
            actionMethod
                .getAnnotation(NextContext::class.java)
                ?.let { nc -> NextActionContext(nc.value, nc.status) }

        actionMethod.getAnnotation(QMsg::class.java)?.let { qq ->
            ai.reply = qq.reply
            ai.at = qq.at
            ai.atNewLine = qq.atNewLine
            ai.mastAtBot = qq.mastAtBot
            if (qq.recall > 0) ai.recall = qq.recall
            ai.forceMatch = qq.forceMatch
        }


        ai.decodeRainCode = actionMethod.getAnnotation(RainCodeString::class.java) != null
        return ai
    }

}