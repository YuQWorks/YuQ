package com.icecreamqaq.yuq.controller

import yuq.controller.router.RouterMatcher
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageItemChain
import com.icecreamqaq.yuq.message.MessageLineQ
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rain.controller.ProcessInvoker
import rain.controller.simple.SimpleActionInvoker
import yuq.controller.MessageChannel
import java.lang.reflect.Method


class BotActionInvoker(
    val channels: Array<MessageChannel>,
    val matchers: List<RouterMatcher>,
    action: ProcessInvoker<BotActionContext>,
    beforeProcesses: Array<ProcessInvoker<BotActionContext>>,
    aftersProcesses: Array<ProcessInvoker<BotActionContext>>,
    catchsProcesses: Array<ProcessInvoker<BotActionContext>>,
    actionClass: Class<*>? = null,
    actionMethod: Method? = null
) : SimpleActionInvoker<BotActionContext>(actionClass, actionMethod, action, beforeProcesses, aftersProcesses, catchsProcesses) {


    override suspend fun onActionResult(context: BotActionContext, result: Any?): Boolean {
        if (result == null) return false
        if (checkResult(context, result)) return true

        context.result = when (result) {
            is String -> result.toMessage()
            is Message -> result
            is MessageItem -> result.toMessage()
            is MessageItemChain -> result.toMessage()
            is MessageLineQ -> result.toMessage()
            is Array<*> ->
                coroutineScope {
                    launch {
                        result.forEach {
                            when (it) {
                                is Int -> delay(it.toLong())
                                is Long -> delay(it)
                                is Message -> context.source.sendMessage(it)
                            }
                        }
                    }
                }

            else -> result.toString().toMessage()
        }
        return false
    }

    override suspend fun checkChannel(context: BotActionContext): Boolean =
        context.channel in channels

}
