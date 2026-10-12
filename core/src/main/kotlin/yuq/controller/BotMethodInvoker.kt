package yuq.controller

import rain.controller.ControllerInstanceGetter
import rain.controller.simple.SimpleKJReflectMethodInvoker
import yuq.Bot
import yuq.contact.Contact
import yuq.contact.ContactSession
import yuq.contact.Group
import yuq.contact.GroupMember
import yuq.message.Message
import yuq.message.MessageSource
import yuq.message.chain.MessageBody
import yuq.message.items.At
import java.lang.reflect.Method

open class BotMethodInvoker(
    method: Method,
    instance: ControllerInstanceGetter
) : SimpleKJReflectMethodInvoker<BotActionContext, (BotActionContext) -> Any?>(method, instance) {

    override fun getParam(param: MethodParam<(BotActionContext) -> Any?>, context: BotActionContext): Any? =
        param.attachment?.invoke(context)

    override fun initParam(method: Method, params: Array<MethodParam<(BotActionContext) -> Any?>>) {
        params.forEach { param ->
            param.attachment = { context ->
                val value = context[param.name] ?: when {
                    param.type == BotActionContext::class.java -> context
                    param.type == Bot::class.java -> context.bot
                    param.type == Message::class.java -> context.message
                    param.type == MessageBody::class.java -> context.message.body
                    param.type == MessageSource::class.java -> context.message.source
                    param.type == MessageChannel::class.java -> context.channel
                    param.type == ContactSession::class.java -> context.source.session
                    Throwable::class.java.isAssignableFrom(param.type) -> context.runtimeError
                    Contact::class.java.isAssignableFrom(param.type) ->
                        when (param.name) {
                            "source", "contact" -> context.source
                            "group" -> (context.source as? Group) ?: (context.sender as? GroupMember)?.group
                            else -> context.sender
                        }.takeIf { param.type.isInstance(it) }
                    else -> null
                }
                convert(value, param.type)
            }
        }
    }

    private fun convert(value: Any?, type: Class<*>): Any? {
        if (value == null || type.isInstance(value)) return value
        val text = when (value) {
            is String -> value
            is At -> value.target
            else -> return value
        }
        return when {
            type == String::class.java -> text
            type.isEnum -> type.enumConstants.firstOrNull { (it as Enum<*>).name.equals(text, ignoreCase = true) }
                ?: error("Invalid value '$text' for enum ${type.name}")
            else -> text.stringAsSimple(type) ?: value
        }
    }
}
