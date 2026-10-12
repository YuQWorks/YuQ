package yuq.controller

import kotlinx.coroutines.delay
import rain.controller.ProcessInvoker
import rain.controller.simple.SimpleActionInvoker
import yuq.controller.router.RouterMatcher
import yuq.message.Message
import yuq.message.SendAble
import yuq.message.items.Text
import java.lang.reflect.Method


open class BotActionInvoker(
    override val actionClass: Class<*>,
    override val actionMethod: Method,
    val channels: Array<MessageChannel>,
    val matchers: List<RouterMatcher>,
    action: ProcessInvoker<BotActionContext>,
    beforeProcesses: Array<ProcessInvoker<BotActionContext>>,
    afterProcesses: Array<ProcessInvoker<BotActionContext>>,
    catchProcesses: Array<ProcessInvoker<BotActionContext>>
) : SimpleActionInvoker<BotActionContext>(actionClass, actionMethod, action, beforeProcesses, afterProcesses, catchProcesses) {


    override suspend fun onActionResult(context: BotActionContext, result: Any?): Boolean {
        if (result == null) return false
        if (checkResult(context, result)) return true

        context.result = when (result) {
            is String -> Text(result).toMessage()
            is Message -> result
            is SendAble -> result.toMessage()
            is Array<*> -> {
                result.forEach {
                    when (it) {
                        is Int -> delay(it.toLong())
                        is Long -> delay(it)
                        is Message -> context.source.sendMessage(it)
                        is SendAble -> context.source.sendMessage(it)
                        is String -> context.source.sendMessage(it)
                    }
                }
                null
            }
            else -> Text(result.toString()).toMessage()
        }
        return false
    }

    override suspend fun checkChannel(context: BotActionContext): Boolean =
        context.channel in channels

    override suspend fun checkActionResult(context: BotActionContext): Boolean {
        if (!super.checkActionResult(context)) return false
        (context.result as? Message)?.let { context.source.sendMessage(it) }
        return true
    }

}
