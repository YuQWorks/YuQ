package yuq.controller

import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.BotService
import com.icecreamqaq.yuq.contact.Friend
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.MessageFailByCancel
import com.icecreamqaq.yuq.message.MessageFailByReadTimeOut
import com.icecreamqaq.yuq.message.Text
import rain.di.config.impl.ConfigImpl
import rain.function.type.RelType
import java.lang.reflect.Proxy
import kotlin.test.*

class LegacyApiAdaptationTest {
    @Test
    fun `legacy failed sources retain the sending bot and destination`() {
        val bot = stub(Bot::class.java, mapOf("getBotId" to 42L))
        val contact = stub(Friend::class.java, mapOf("getBot" to bot, "getId" to 123L))
        val cancelled = MessageFailByCancel(contact, "cancelled")
        val timeout = MessageFailByReadTimeOut(contact, "timeout")
        assertEquals(42L, cancelled.sender)
        assertEquals(42L, timeout.sender)
        assertEquals(123L, cancelled.sendTo)
        assertEquals("cancelled", cancelled.liteMsg)
        assertEquals(-1, cancelled.id)
    }

    @Test
    fun `legacy message factory and per-bot sessions remain available`() {
        val message: Message = "hello".toMessage()
        assertEquals("hello", (message.body[0] as Text).text)
        val first = stub(Bot::class.java, emptyMap())
        val second = stub(Bot::class.java, emptyMap())
        val session = BotService.getContextSession(first, "same-id")
        assertSame(session, BotService.getContextSession(first, "same-id"))
        assertNotSame(session, BotService.getContextSession(second, "same-id"))
    }

    @Test
    fun `Rain config scans both namespaces and exposes the strict setting`() {
        val config = ConfigImpl(javaClass.classLoader, "test", null).init()
        val packages = config.getArray("rain.scanPackages", RelType.create<String>(String::class.java))
        assertTrue("rain" in packages)
        assertTrue("yuq" in packages)
        assertTrue("com.icecreamqaq.yuq" in packages)
        assertEquals(true, config.getConfig("yuq.chat.strict", RelType.create<Boolean>(Boolean::class.javaObjectType)))
    }

    private fun <T> stub(type: Class<T>, values: Map<String, Any?>): T =
        type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when (method.name) {
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.get(0)
                "toString" -> "Stub(${type.simpleName})"
                else -> if (method.name in values) values[method.name] else error("Unexpected call: ${method.name}")
            }
        })
}
