package yuq.controller

import rain.controller.ControllerInstanceGetter
import rain.controller.simple.SimpleKJReflectMethodInvoker
import rain.api.annotation.Nullable
import rain.function.type.RelType
import yuq.Bot
import yuq.contact.Contact
import yuq.contact.ContactSession
import yuq.contact.Group
import yuq.contact.GroupMember
import yuq.message.Message
import yuq.message.MessageItem
import yuq.message.MessageSource
import yuq.message.chain.MessageBody
import yuq.message.items.Text
import java.lang.reflect.Method

open class BotMethodInvoker(
    method: Method,
    instance: ControllerInstanceGetter,
    private val converter: BotParameterConverter = BotParameterConverter()
) : SimpleKJReflectMethodInvoker<BotActionContext, (BotActionContext) -> Any?>(method, instance) {

    data class ParameterBinding(val name: String, val type: RelType<*>, val nullable: Boolean, val hasDefault: Boolean) {
        val canBeAbsent: Boolean get() = nullable || hasDefault
    }

    private val parameterBindings = HashMap<String, ParameterBinding>()
    fun parameterBinding(name: String): ParameterBinding? = parameterBindings[name]
    fun parameterType(name: String): RelType<*>? = parameterBinding(name)?.type

    override fun getParam(param: MethodParam<(BotActionContext) -> Any?>, context: BotActionContext): Any? =
        param.attachment?.invoke(context)

    override fun initParam(method: Method, params: Array<MethodParam<(BotActionContext) -> Any?>>) {
        params.forEach { param ->
            // Rain's Java invoker currently exposes a raw class; preserve generic collection elements here.
            val relType = param.reflectParam?.parameterizedType?.let(RelType.Companion::create) ?: param.relType
            // Kotlin reflection also wraps Java methods, but their platform types do not reflect @Nullable.
            val nullable = param.nullable || method.declaringClass.getAnnotation(Metadata::class.java) == null &&
                param.hasAnnotation(Nullable::class.java)
            if (nullable && !param.nullable) {
                val checker = param.valueChecker
                param.valueChecker = { value -> if (value == null) null else checker(value) }
            }
            parameterBindings[param.name] = ParameterBinding(param.name, relType, nullable, param.optional)
            param.attachment = { context ->
                val value = if (context.contains(param.name)) context[param.name] else when {
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
                if (value == null || param.type.isInstance(value)) value
                else when (value) {
                    is String -> converter.convert(MessageBody().append(Text(value)), relType, context)
                    is MessageItem -> converter.convert(MessageBody().append(value), relType, context)
                    else -> value
                }
            }
        }
    }

}
