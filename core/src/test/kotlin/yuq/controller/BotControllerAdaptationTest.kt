package yuq.controller

import kotlinx.coroutines.runBlocking
import rain.api.di.DiContext
import rain.api.loader.LoadItem
import rain.controller.annotation.After
import rain.controller.annotation.Before
import rain.controller.annotation.Catch
import rain.controller.annotation.Global
import rain.controller.annotation.Path
import rain.controller.annotation.ProcessBy
import rain.controller.ProcessInvoker
import rain.controller.ProcessProvider
import rain.controller.special.ActionResult
import rain.controller.special.SkipMe
import yuq.Bot
import yuq.annotation.BotAction
import yuq.annotation.BotController
import yuq.annotation.GroupAction
import yuq.annotation.GroupController
import yuq.contact.ContactSession
import yuq.contact.Friend
import yuq.message.Message
import yuq.message.MessageSource
import yuq.message.chain.MessageBody
import yuq.message.items.At
import yuq.message.items.Image
import yuq.message.items.Text
import yuq.controller.router.BotRouter
import yuq.controller.router.ParameterMatcher
import yuq.controller.router.StaticMatcher
import java.io.File
import java.lang.reflect.Method
import kotlin.test.*

class BotControllerAdaptationTest {

    @Test
    fun `load and invoke action with default before and after processes`() = runBlocking {
        val controller = HelloController()
        val loader = load(controller)
        val context = context("hello", MessageChannel.Group)
        assertTrue(loader.rootRouter!!(context))
        assertEquals(listOf("before", "action", "after"), controller.calls)
        assertEquals(listOf("hello back"), replies(context))
        assertSame(controller, loader.findRootRouter("").controllers.single().controllerInstance)
        assertEquals(1, loader.rootRouter!!.actions.size)
    }

    @Test
    fun `channel and exact command checks reject unrelated messages`() = runBlocking {
        val controller = HelloController()
        val router = load(controller).rootRouter!!
        assertFalse(router(context("hello", MessageChannel.Friend)))
        assertFalse(router(context("helloworld", MessageChannel.Group)))
        assertFalse(router(context("hello extra", MessageChannel.Group)))
        assertFalse(router(context("", MessageChannel.Group)))
        assertTrue(controller.calls.isEmpty())
    }

    @Test
    fun `controller prefix binds parameters for Kotlin suspend actions`() = runBlocking {
        val router = load(PrefixedController()).rootRouter!!
        val context = context("admin add 20 22", MessageChannel.Group)
        assertTrue(router(context))
        assertEquals(listOf("42"), replies(context))
        val absolute = context("status", MessageChannel.Group)
        assertTrue(router(absolute))
        assertEquals(listOf("ready"), replies(absolute))
    }

    @Test
    fun `parameter branches restore values before falling back`() = runBlocking {
        val router = load(BranchController()).rootRouter!!
        // Force a partial match to fail before trying an otherwise valid route.
        router.router.subRouters.add(0, BotRouter(ParameterMatcher("leaked")).apply {
            subRouters.add(BotRouter(StaticMatcher("missing")))
        })
        val context = context("value", MessageChannel.Friend)
        assertTrue(router(context))
        assertEquals(listOf("clean"), replies(context))
        assertNull(context["leaked"])
    }

    @Test
    fun `SkipMe restores the context before the next matching action`() = runBlocking {
        val controller = SkippingController()
        val router = load(controller).rootRouter!!
        val skipped = router.router.subRouters.single { it.matcher == ParameterMatcher("skipped") }
        router.router.subRouters.remove(skipped)
        router.router.subRouters.add(0, skipped)
        val context = context("value", MessageChannel.Friend)
        assertTrue(router(context))
        assertEquals(1, controller.skipCalls)
        assertEquals(listOf("clean"), replies(context))
        assertNull(context["skipped"])
    }

    @Test
    fun `matcher traverses split text and non-text arguments`() = runBlocking {
        val router = load(ArgumentController()).rootRouter!!
        val context = context("", MessageChannel.Friend, MessageBody()
            .append(Text("pi")).append(Text("ng ")).append(At("12345")))
        assertTrue(router(context))
        assertEquals(listOf("12345:default"), replies(context))
    }

    @Test
    fun `catch handler receives original exception and produces a reply`() = runBlocking {
        val router = load(FailingController()).rootRouter!!
        val context = context("fail", MessageChannel.Friend)
        assertTrue(router(context))
        assertEquals(listOf("caught:bad input"), replies(context))
        assertNull(context.runtimeError)
    }

    @Test
    fun `global process applies across separate loader batches without duplication`() = runBlocking {
        val global = GlobalController()
        val action = HelloController()
        val di = TestDiContext(global, action)
        val loader = BotControllerLoader(di)
        loader.load(listOf(loadItem(action)))
        loader.load(listOf(loadItem(global)))
        val context = context("hello", MessageChannel.Group)
        assertTrue(loader.rootRouter!!(context))
        assertEquals(1, global.calls)
        assertEquals(listOf("hello back"), replies(context))
    }

    @Test
    fun `external ProcessBy participates in the ordered Rain flow`() = runBlocking {
        val controller = ProvidedController()
        val provider = ProvidedProcess()
        val loader = BotControllerLoader(TestDiContext(controller, provider))
        loader.load(listOf(loadItem(controller)))
        val context = context("provided", MessageChannel.Friend)
        assertTrue(loader.rootRouter!!(context))
        assertSame(controller, provider.controller)
        assertEquals(listOf("external", "local", "action"), controller.calls)
        assertEquals(listOf("provided back"), replies(context))
    }

    @Test
    fun `process only and except filters apply to the intended actions`() = runBlocking {
        val controller = FilteredController()
        val router = load(controller).rootRouter!!
        assertTrue(router(context("one", MessageChannel.Friend)))
        assertTrue(router(context("two", MessageChannel.Friend)))
        assertEquals(listOf("only-one", "except-one"), controller.calls)
    }

    @Test
    fun `legacy annotations load through the new implementation`() = runBlocking {
        val controller = LegacyAnnotatedController()
        val loader = com.icecreamqaq.yuq.controller.BotControllerLoader(TestDiContext(controller))
        loader.load(listOf(loadItem(controller)))
        val context = context("legacy", MessageChannel.Group)
        assertTrue(loader.rootRouter!!(context))
        assertEquals(listOf("still here"), replies(context))
    }

    @Test
    fun `Java method parameters use named route values and injected context`() = runBlocking {
        val router = load(JavaTestController()).rootRouter!!
        val context = context("java 7", MessageChannel.Friend)
        assertTrue(router(context))
        assertEquals(listOf("7:friend"), replies(context))
    }

    @Test
    fun `reply arrays send each item without producing an extra message`() = runBlocking {
        val router = load(ArrayController()).rootRouter!!
        val context = context("array", MessageChannel.Friend)
        assertTrue(router(context))
        assertEquals(listOf("first", "second"), replies(context))
        assertNull(context.result)
    }

    @Test
    fun `context user stays optional for platform contacts`() {
        assertNull(context("hello", MessageChannel.Friend).user)
    }

    @GroupController
    class HelloController {
        val calls = mutableListOf<String>()
        @Before fun before() { calls += "before" }
        @GroupAction("hello") fun hello(): String { calls += "action"; return "hello back" }
        @After fun after() { calls += "after" }
    }

    @GroupController
    @Path("admin")
    class PrefixedController {
        @GroupAction("add {left} {right}")
        suspend fun add(left: Int, right: Int): String = (left + right).toString()
        @GroupAction("/status") fun status() = "ready"
    }

    @BotController
    class BranchController {
        @BotAction("{clean}") fun fallback(context: BotActionContext) =
            if (context["leaked"] == null) "clean" else "dirty"
    }

    @BotController
    class SkippingController {
        var skipCalls = 0
        @BotAction("{skipped}") fun skip(): Any { skipCalls++; return SkipMe }
        @BotAction("{clean}") fun fallback(context: BotActionContext) =
            if (context["skipped"] == null) "clean" else "dirty"
    }

    @BotController
    class ArgumentController {
        @BotAction("ping {target}") fun ping(target: String, optional: String = "default") = "$target:$optional"
    }

    @BotController
    class FailingController {
        @BotAction("fail") fun fail(): String = throw IllegalArgumentException("bad input")
        @Catch(error = IllegalArgumentException::class)
        fun recover(error: IllegalArgumentException): Nothing = throw ActionResult("caught:${error.message}")
    }

    @BotController
    class GlobalController {
        var calls = 0
        @Global @Before fun before() { calls++ }
    }

    @ProcessBy(ProvidedProcess::class)
    annotation class Provided

    @Before(weight = -10)
    class ProvidedProcess : ProcessProvider<BotActionContext> {
        var controller: Any? = null
        override fun <T> invoke(
            provideAnnotation: Annotation,
            functionAnnotation: Annotation,
            controllerClass: Class<T>,
            controllerInstance: T,
            action: Method?
        ): ProcessInvoker<BotActionContext> {
            controller = controllerInstance
            return ProcessInvoker {
                (controllerInstance as ProvidedController).calls += "external"
                null
            }
        }
    }

    @BotController
    @Provided
    class ProvidedController {
        val calls = mutableListOf<String>()
        @Before fun before() { calls += "local" }
        @BotAction("provided") fun action(): String { calls += "action"; return "provided back" }
    }

    @BotController
    class FilteredController {
        val calls = mutableListOf<String>()
        @Before(only = ["one"]) fun onlyOne() { calls += "only-one" }
        @Before(except = ["one"]) fun exceptOne() { calls += "except-one" }
        @BotAction("one") fun one() = "one"
        @BotAction("two") fun two() = "two"
    }

    @com.icecreamqaq.yuq.annotation.GroupController
    class LegacyAnnotatedController {
        @com.icecreamqaq.yuq.annotation.GroupAction("legacy") fun legacy() = "still here"
    }

    @BotController
    class ArrayController {
        @BotAction("array") fun array(): Array<Any> = arrayOf("first", 0, Text("second").toMessage())
    }

    private fun load(vararg controllers: Any): BotControllerLoader =
        BotControllerLoader(TestDiContext(*controllers)).also { it.load(controllers.map(::loadItem)) }

    private fun loadItem(controller: Any): LoadItem {
        val type = controller.javaClass
        val annotation = type.annotations.first { it.annotationClass.java.getAnnotation(rain.api.annotation.LoadBy::class.java) != null }
        return LoadItem(type, annotation.annotationClass.java, annotation)
    }

    private fun context(text: String, channel: MessageChannel, body: MessageBody? = null): BotActionContext {
        val contact = RecordingFriend()
        return BotActionContext(contact.bot, channel, contact, contact, body?.toMessage() ?: Text(text).toMessage())
    }

    private fun replies(context: BotActionContext) = (context.source as RecordingFriend).sent.map { message ->
        message.body.joinToString("") { (it as Text).text }
    }

    private class TestDiContext(vararg instances: Any) : DiContext {
        private val beans: MutableMap<Class<*>, Any> = HashMap<Class<*>, Any>().apply {
            instances.forEach { put(it.javaClass, it) }
        }
        override fun <T> getBean(clazz: Class<T>, instanceName: String): T? = beans[clazz]?.let(clazz::cast)
        override fun <T> putBean(clazz: Class<T>, instanceName: String, instance: T): T {
            beans[clazz] = instance as Any
            return instance
        }
        override fun <T> newBean(clazz: Class<T>): T = error("Unexpected construction of ${clazz.name}")
        override fun <T : Any> injectBean(bean: T): T = bean
        override fun <T : Any> forceInjectBean(bean: T): T = bean
        override fun registerClass(clazz: Class<*>) {}
    }

    private class RecordingFriend : Friend {
        override val platform = "test"
        override val runtime = "test"
        override val id = "friend"
        override val nickname = "tester"
        override val avatar = ""
        override val guid = "test:friend"
        override val session = ContactSession(guid)
        override val logString = nickname
        override val bot = object : Bot {
            override val platform = "test"
            override val runtime = "test"
            override val id = "bot"
            override val guid = "test:bot"
        }
        val sent = mutableListOf<Message>()
        override fun sendMessage(message: Message): MessageSource {
            sent += message
            return object : MessageSource {
                override val id = sent.size.toString()
                override suspend fun recall() {}
            }
        }
        override fun uploadImage(imageFile: File): Image = error("Unused in controller tests")
        override fun sendFile(file: File) {}
        override fun delete() {}
    }
}
