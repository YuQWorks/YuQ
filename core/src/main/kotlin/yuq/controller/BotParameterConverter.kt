package yuq.controller

import rain.function.type.RelType
import yuq.contact.Account
import yuq.contact.Group
import yuq.contact.GroupMember
import yuq.message.Message
import yuq.message.MessageItem
import yuq.message.MessageItemChain
import yuq.message.chain.MessageBody
import yuq.message.items.At
import yuq.message.items.Text
import java.lang.reflect.Array as ReflectArray
import java.util.concurrent.ConcurrentHashMap

/** Convert a consumed message range. Null means this route candidate does not match. */
open class BotParameterConverter {
    private val converters = ConcurrentHashMap<Class<*>, (MessageBody, BotActionContext) -> Any?>()
    private val handlers = ConcurrentHashMap<Class<*>, BotParameterHandler<*>>()

    fun <T : Any> register(type: Class<T>, converter: (MessageBody, BotActionContext) -> T?) {
        converters[boxed(type)] = converter
    }

    fun <T : Any> registerHandler(type: Class<T>, handler: BotParameterHandler<T>) {
        handlers[boxed(type)] = handler
    }

    /** A fixed text format consumes one prefix across adjacent Text items. Rich items stay intact. */
    fun <T : Any> registerPattern(type: Class<T>, pattern: Regex, convert: (MatchResult) -> T?) {
        registerHandler(type) { input ->
            sequence {
                val match = pattern.find(input.cursor.remainingText())
                    ?.takeIf { it.range.first == 0 && it.value.isNotEmpty() } ?: return@sequence
                val value = convert(match) ?: return@sequence
                if (input.cursor.isNext(match.value)) yield(BotParameterCandidate(input.cursor.mark(), value))
            }
        }
    }

    internal fun handler(type: RelType<*>?): BotParameterHandler<*>? = type?.let { handlers[boxed(it.realClass)] }
    internal fun accepts(type: RelType<*>, value: Any): Boolean = boxed(type.realClass).isInstance(value)

    internal fun elementType(type: RelType<*>): RelType<*>? = when {
        type.realClass.isArray -> RelType.create(type.realClass.componentType)
        type.realClass in listOf(List::class.java, Collection::class.java, Set::class.java) -> type.generics?.firstOrNull()
        else -> null
    }

    internal fun collect(values: List<Any>, type: RelType<*>): Any = when {
        type.realClass.isArray -> ReflectArray.newInstance(type.realClass.componentType, values.size).also { array ->
            values.forEachIndexed { index, value -> ReflectArray.set(array, index, value) }
        }
        type.realClass == Set::class.java -> values.toSet()
        else -> values.toList()
    }

    open fun convert(content: MessageBody, type: RelType<*>?, context: BotActionContext): Any? {
        if (content.isEmpty()) return null
        if (type == null || type.realClass == Any::class.java) {
            return asText(content) ?: if (content.size == 1) content[0] else content
        }
        val target = boxed(type.realClass)
        converters[target]?.let { return it(content, context)?.takeIf(target::isInstance) }
        if (target == MessageBody::class.java || target == MessageItemChain::class.java) return content
        if (target == Message::class.java) return content.toMessage().apply { source = context.message.source }
        if (target.isArray) {
            val values = elements(content, RelType.create(target.componentType), context) ?: return null
            return ReflectArray.newInstance(target.componentType, values.size).also { array ->
                values.forEachIndexed { index, value -> ReflectArray.set(array, index, value) }
            }
        }
        if (target == List::class.java || target == Collection::class.java || target == Set::class.java) {
            val elementType = type.generics?.firstOrNull() ?: return null
            val values = elements(content, elementType, context) ?: return null
            return if (target == Set::class.java) values.toSet() else values
        }
        if (target == Map::class.java) return map(content, type, context)
        if (MessageItem::class.java.isAssignableFrom(target)) {
            if (content.size == 1 && target.isInstance(content[0])) return content[0]
            return asText(content)?.let(::Text)?.takeIf(target::isInstance)
        }
        val text = asText(content) ?: (content.first() as? At)?.takeIf { content.size == 1 }?.target ?: return null
        if (Account::class.java.isAssignableFrom(target)) {
            val group = (context.source as? Group) ?: (context.sender as? GroupMember)?.group
            val member = group?.members?.get(text)
                ?: (context.sender as? GroupMember)?.takeIf { it.id == text }
            return listOfNotNull(member, context.sender, context.source, group)
                .firstOrNull { it.id == text && target.isInstance(it) }
        }
        return when {
            target == String::class.java -> text
            target == Boolean::class.javaObjectType -> text.toBooleanStrictOrNull()
            target == Byte::class.javaObjectType -> text.toByteOrNull()
            target == Short::class.javaObjectType -> text.toShortOrNull()
            target == Int::class.javaObjectType -> text.toIntOrNull()
            target == Long::class.javaObjectType -> text.toLongOrNull()
            target == Float::class.javaObjectType -> text.toFloatOrNull()
            target == Double::class.javaObjectType -> text.toDoubleOrNull()
            target == Char::class.javaObjectType -> text.singleOrNull()
            target.isEnum -> target.enumConstants.firstOrNull { (it as Enum<*>).name.equals(text, ignoreCase = true) }
            else -> null
        }
    }

    private fun elements(content: MessageBody, elementType: RelType<*>, context: BotActionContext): List<Any>? {
        val result = ArrayList<Any>()
        for (item in content) {
            val value = convert(MessageBody().append(item), elementType, context)
            if (value == null) {
                if (item is Text && item.text.isBlank()) continue
                return null
            }
            result.add(value)
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun map(content: MessageBody, type: RelType<*>, context: BotActionContext): Map<String, Any>? {
        val keyType = type.generics?.getOrNull(0) ?: return null
        val valueType = type.generics?.getOrNull(1) ?: return null
        if (keyType.realClass != String::class.java) return null
        val text = asText(content) ?: return null
        val valueClass = boxed(valueType.realClass)
        val values = when {
            valueClass.isEnum -> Regex(valueClass.enumConstants.map { (it as Enum<*>).name }
                .sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) })
            valueClass == Boolean::class.javaObjectType -> Regex("true|false")
            Number::class.java.isAssignableFrom(valueClass) -> Regex("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?")
            else -> return null
        }
        val result = LinkedHashMap<String, Any>()
        var offset = 0
        for (match in values.findAll(text)) {
            val key = text.substring(offset, match.range.first).trim()
            if (key.isEmpty() || key in result) return null
            result[key] = convert(MessageBody().append(Text(match.value)), valueType, context) ?: return null
            offset = match.range.last + 1
        }
        return result.takeIf { it.isNotEmpty() && text.substring(offset).isBlank() }
    }

    private fun asText(content: MessageBody): String? {
        val text = StringBuilder()
        for (item in content) {
            if (item !is Text) return null
            text.append(item.text)
        }
        return text.toString()
    }

    private fun boxed(type: Class<*>): Class<*> = when (type) {
        Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
        Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
        Short::class.javaPrimitiveType -> Short::class.javaObjectType
        Int::class.javaPrimitiveType -> Int::class.javaObjectType
        Long::class.javaPrimitiveType -> Long::class.javaObjectType
        Float::class.javaPrimitiveType -> Float::class.javaObjectType
        Double::class.javaPrimitiveType -> Double::class.javaObjectType
        Char::class.javaPrimitiveType -> Char::class.javaObjectType
        else -> type
    }
}
