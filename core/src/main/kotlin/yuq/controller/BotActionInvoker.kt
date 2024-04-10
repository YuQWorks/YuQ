package yuq.controller

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rain.controller.ProcessInvoker
import rain.controller.simple.SimpleActionInvoker
import yuq.controller.router.RouterMatcher
import yuq.message.Message
import yuq.message.MessageItem
import yuq.message.MessageItemChain
import yuq.message.chain.MessageBody
import yuq.message.items.Text


class BotActionInvoker(
    val channels: Array<MessageChannel>,
    val matchers: List<RouterMatcher>,
    action: ProcessInvoker<BotActionContext>,
    beforeProcesses: Array<ProcessInvoker<BotActionContext>>,
    aftersProcesses: Array<ProcessInvoker<BotActionContext>>,
    catchsProcesses: Array<ProcessInvoker<BotActionContext>>
) : SimpleActionInvoker<BotActionContext>(action, beforeProcesses, aftersProcesses, catchsProcesses) {


    override suspend fun onActionResult(context: BotActionContext, result: Any?): Boolean {
        if (result == null) return false
        if (checkResult(context, result)) return true

        context.result = when (result) {
            is String -> Text(result).toMessage()
            is Message -> result
            is MessageItem -> result.toMessage()
            is MessageBody -> result.toMessage()
//            is MessageLineQ -> result.toMessage()
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

            else -> Text(result.toString()).toMessage()
        }
        return false
    }

    override suspend fun checkChannel(context: BotActionContext): Boolean =
        context.channel in channels

}
