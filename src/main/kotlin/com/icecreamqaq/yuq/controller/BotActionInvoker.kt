package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ActionInvoker
import com.IceCreamQAQ.Yu.controller.ProcessInvoker
import com.IceCreamQAQ.Yu.controller.simple.SimpleActionInvoker
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageItemChain
import com.icecreamqaq.yuq.message.MessageLineQ


class BotActionInvoker(
    action: ProcessInvoker<BotActionContext>,
    beforeProcesses: Array<ProcessInvoker<BotActionContext>>,
    aftersProcesses: Array<ProcessInvoker<BotActionContext>>,
    catchsProcesses: Array<ProcessInvoker<BotActionContext>>
) : SimpleActionInvoker<BotActionContext>(action, beforeProcesses, aftersProcesses, catchsProcesses) {


    override fun onActionResult(context: BotActionContext, result: Any?): Boolean {
        if (result == null) return false
        if (checkResult(context, result)) return true

        context.result = when (result) {
            is String -> result.toMessage()
            is Message -> result
            is MessageItem -> result.toMessage()
            is MessageItemChain -> result.toMessage()
            is MessageLineQ -> result.toMessage()
//            is Array<*> ->
//                result.forEach {
//                    when(it){
//                        is Int ->
//                    }
//                }
            else -> result.toString().toMessage()
        }
        return false
    }

}
