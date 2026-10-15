package yuq.controller

import kotlinx.coroutines.runBlocking
import rain.api.annotation.LoadBy
import rain.api.di.DiContext
import rain.api.loader.LoadItem
import rain.controller.annotation.Before
import rain.controller.annotation.Catch
import rain.controller.annotation.Path
import rain.controller.special.SkipMe
import yuq.Bot
import yuq.annotation.BotAction
import yuq.annotation.BotController
import yuq.contact.AccountList
import yuq.contact.ContactSession
import yuq.contact.Friend
import yuq.contact.Group
import yuq.contact.GroupMember
import yuq.controller.router.BotRootRouter
import yuq.controller.router.BotRouter
import yuq.controller.router.ConsumptionMatcher
import yuq.controller.router.RouterMatcher
import yuq.message.Message
import yuq.message.MessageItem
import yuq.message.MessageSource
import yuq.message.chain.MessageBody
import yuq.message.items.At
import yuq.message.items.Image
import yuq.message.items.Text
import java.io.File
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.util.AbstractMap
import kotlin.random.Random
import kotlin.test.*

class DynamicMessageMatchingTest {
    @Test
    fun `adjacent literal and typed parameter consume text without spaces`() = runBlocking {
        val router = load(AdjacentController())
        val context = context("数值60结束")
        assertTrue(router(context))
        assertEquals(listOf("60"), replies(context))
    }

    @Test
    fun `declaration spaces group consumers while input gaps are optional`() = runBlocking {
        val loader = BotControllerLoader(TestDiContext(SpacedController()))
        loader.load(listOf(item(SpacedController::class.java)))
        val action = loader.rootRouter!!.actions.single()
        assertEquals(3, action.matchers.size)
        assertTrue(action.matchers.all { it is ConsumptionMatcher })
        for (input in listOf("禁言60秒", "禁言 60 秒", "禁言\t60\n秒")) {
            val context = context(input)
            assertTrue(loader.rootRouter!!(context))
            assertEquals(listOf("60"), replies(context))
        }
    }

    @Test
    fun `literal suffix failure retries a longer string consumption in the same group`() = runBlocking {
        val controller = BacktrackingController()
        val router = load(controller)
        val context = context("说先再加不对再加12")
        assertTrue(router(context))
        assertEquals(listOf("先再加不对:12"), replies(context))
        assertEquals(1, controller.beforeCalls)
        assertEquals(0, controller.catchCalls)
    }

    @Test
    fun `strings preserve spaces within consumed text`() = runBlocking {
        val router = load(EchoController())
        val context = context("说hello world 你好")
        assertTrue(router(context))
        assertEquals(listOf("hello world 你好"), replies(context))
    }

    @Test
    fun `numeric mismatch tries another controller without executing processes`() = runBlocking {
        val numeric = NumericController()
        val router = load(numeric, TextController())
        val context = context("值不是数字")
        assertTrue(router(context))
        assertEquals(listOf("text:不是数字"), replies(context))
        assertEquals(0, numeric.beforeCalls)
        assertEquals(0, numeric.catchCalls)
        assertNull(context.runtimeError)
    }

    @Test
    fun `SkipMe does not invoke the same action again for another valid parameter partition`() = runBlocking {
        val skipping = SkippingPartitionController()
        val router = load(skipping, PartitionFallbackController())
        val context = context("跳过123")
        assertTrue(router(context))
        assertEquals(1, skipping.calls)
        assertEquals(listOf("123"), replies(context))
        assertNull(context["left"])
        assertNull(context["right"])
        context.cursor.reMark(0 to 0)
        assertTrue(router(context))
        assertEquals(2, skipping.calls)
    }

    @Test
    fun `overflow and invalid booleans reject candidates before invocation`() = runBlocking {
        val controller = StrictTypesController()
        val router = load(controller)
        assertFalse(router(context("byte128")))
        assertFalse(router(context("booleanmaybe")))
        assertFalse(router(context("enum未知")))
        assertEquals(0, controller.calls)
        val valid = context("booleanfalse")
        assertTrue(router(valid))
        assertEquals(listOf("false"), replies(valid))
    }

    @Test
    fun `failed aggregate restores cursor values result and invoker`() = runBlocking {
        val router = load(BacktrackingController())
        val context = context("说你好再加不是数字")
        context["kept"] = "original"
        context.result = "previous result"
        val error = IllegalStateException("previous error")
        context.runtimeError = error
        context.actionInvoker = router.actions.single()
        val position = context.cursor.mark()
        assertFalse(router(context))
        assertEquals(position, context.cursor.mark())
        assertEquals("original", context["kept"])
        assertNull(context["text"])
        assertNull(context["number"])
        assertEquals("previous result", context.result)
        assertSame(error, context.runtimeError)
        assertSame(router.actions.single(), context.actionInvoker)
    }

    @Test
    fun `text parameters and literal suffixes span adjacent Text items`() = runBlocking {
        val router = load(AdjacentController())
        val body = MessageBody().append(Text("数")).append(Text("值6")).append(Text("0")).append(Text("结束"))
        val context = context(body)
        assertTrue(router(context))
        assertEquals(listOf("60"), replies(context))
    }

    @Test
    fun `At converts to GroupMember in an unspaced mixed message`() = runBlocking {
        val fixture = groupFixture()
        val controller = BanController()
        val router = load(controller)
        val body = MessageBody().append(Text("给")).append(At("42")).append(Text("禁言60秒"))
        val context = fixture.context(body)
        assertTrue(router(context))
        assertSame(fixture.members["42"], controller.member)
        assertEquals(listOf("42:60"), replies(context))
    }

    @Test
    fun `five ban consumption groups accept every optional gap combination around At`() = runBlocking {
        val fixture = groupFixture()
        val controller = BanController()
        val router = load(controller)
        assertEquals(5, router.actions.single().matchers.size)
        for (mask in 0 until 16) {
            fixture.recorder.sent.clear()
            fun gap(boundary: Int) = if (mask and (1 shl boundary) != 0) " " else ""
            val body = MessageBody().append(Text("给" + gap(0))).append(At("42"))
                .append(Text(gap(1) + "禁言" + gap(2) + "30" + gap(3) + "秒"))
            val context = fixture.context(body)
            assertTrue(router(context), "At gap mask: $mask")
            assertSame(fixture.members["42"], controller.member)
            assertEquals(listOf("42:30"), replies(context))
            assertNull(context.runtimeError)
        }
    }

    @Test
    fun `five ban consumption groups resolve StringId with every optional gap combination`() = runBlocking {
        val fixture = groupFixture(listOf("StringId"))
        val controller = BanController()
        val router = load(controller)
        for (mask in 0 until 16) {
            fixture.recorder.sent.clear()
            fun gap(boundary: Int) = if (mask and (1 shl boundary) != 0) " " else ""
            val input = "给" + gap(0) + "StringId" + gap(1) + "禁言" + gap(2) + "30" + gap(3) + "秒"
            val context = fixture.context(MessageBody().append(Text(input)))
            assertTrue(router(context), "StringId gap mask: $mask")
            assertSame(fixture.members["StringId"], controller.member)
            assertEquals(listOf("StringId:30"), replies(context))
            assertNull(context.runtimeError)
        }
    }

    @Test
    fun `ban handles many gaps and retries a valid member prefix before the real delimiter`() = runBlocking {
        val memberId = "String禁言Id"
        val fixture = groupFixture(listOf(memberId, "String"))
        val controller = CommandMatrixController()
        val router = load(controller)
        for (useAt in listOf(false, true)) {
            val groups = listOf(group("给"), memberGroup(memberId, useAt), group("禁言"), group("30"), group("秒"))
            forEachMessageVariant(groups) { body, label ->
                assertMatrixMatch(fixture, router, body, "$memberId:30", "$useAt / $label", { controller.calls },
                    mapOf("member" to fixture.members.getValue(memberId), "seconds" to 30))
            }
        }
    }

    @Test
    fun `transfer mixes two At or text members with arbitrary group gaps`() = runBlocking {
        val fromId = "Alice的Bob"
        val toId = "SecondId"
        val fixture = groupFixture(listOf(fromId, "Alice", toId))
        val controller = CommandMatrixController()
        val router = load(controller)
        for (fromAt in listOf(false, true)) for (toAt in listOf(false, true)) {
            val groups = listOf(group("把"), memberGroup(fromId, fromAt), group("的"), group("12"),
                group("点"), group("转给"), memberGroup(toId, toAt))
            forEachMessageVariant(groups) { body, label ->
                assertMatrixMatch(fixture, router, body, "$fromId->$toId:12", "$fromAt/$toAt / $label", { controller.calls },
                    mapOf("from" to fixture.members.getValue(fromId), "to" to fixture.members.getValue(toId), "amount" to 12))
            }
        }
    }

    @Test
    fun `reminder preserves text spaces and retries repeated suffixes with At or text recipients`() = runBlocking {
        val memberId = "StringId"
        val text = "hello  world，先完成再继续🙂"
        val fixture = groupFixture(listOf(memberId))
        val controller = CommandMatrixController()
        val router = load(controller)
        for (useAt in listOf(false, true)) {
            val groups = listOf(group("提醒"), memberGroup(memberId, useAt), group("内容"), group(text), group("完成"))
            forEachMessageVariant(groups) { body, label ->
                assertMatrixMatch(fixture, router, body, "$memberId:$text", "$useAt / $label", { controller.calls },
                    mapOf("member" to fixture.members.getValue(memberId), "text" to text))
            }
        }
    }

    @Test
    fun `adjacent member parameters accept all At and text combinations without a literal separator`() = runBlocking {
        val fixture = groupFixture(listOf("FirstId", "SecondId"))
        val controller = CommandMatrixController()
        val router = load(controller)
        for (leftAt in listOf(false, true)) for (rightAt in listOf(false, true)) {
            val groups = listOf(group("配对"), memberGroup("FirstId", leftAt), memberGroup("SecondId", rightAt), group("确认"))
            forEachMessageVariant(groups) { body, label ->
                assertMatrixMatch(fixture, router, body, "FirstId+SecondId", "$leftAt/$rightAt / $label", { controller.calls },
                    mapOf("left" to fixture.members.getValue("FirstId"), "right" to fixture.members.getValue("SecondId")))
            }
        }
    }

    @Test
    fun `Path member and Action nickname share optional gaps and repeated start text`() = runBlocking {
        val memberId = "User设置Id"
        val nickname = "新 昵称确认后"
        val fixture = groupFixture(listOf(memberId, "User"))
        val controller = PrefixedMatrixController()
        val router = load(controller)
        for (useAt in listOf(false, true)) {
            val groups = listOf(group("管理"), memberGroup(memberId, useAt), group("设置"), group("昵称"), group(nickname), group("确认"))
            forEachMessageVariant(groups) { body, label ->
                assertMatrixMatch(fixture, router, body, "$memberId:$nickname", "$useAt / $label", { controller.calls },
                    mapOf("member" to fixture.members.getValue(memberId), "nickname" to nickname))
            }
        }
    }

    @Test
    fun `forwarding preserves mixed At Text payload across every gap and fragmentation layout`() = runBlocking {
        val fixture = groupFixture(listOf("StringId", "FirstId", "SecondId"))
        val controller = CommandMatrixController()
        val router = load(controller)
        val firstAt = At("FirstId")
        val secondAt = At("SecondId")
        val payload = listOf(Text("hello  world"), firstAt, Text("先完成再继续 "), secondAt, Text("🙂正文"))
        for (useAt in listOf(false, true)) {
            val groups = listOf(group("转发"), memberGroup("StringId", useAt), group("内容"), payload, group("完成"))
            forEachMessageVariant(groups) { body, label ->
                val context = assertMatrixMatch(fixture, router, body, "forward:StringId", "$useAt / $label", { controller.calls },
                    mapOf("member" to fixture.members.getValue("StringId")))
                val captured = context["payload"] as MessageBody
                assertEquals("hello  world先完成再继续 🙂正文", captured.filterIsInstance<Text>().joinToString("") { it.text }, label)
                val capturedAts = captured.filterIsInstance<At>()
                assertEquals(2, capturedAts.size, label)
                assertSame(firstAt, capturedAts[0], label)
                assertSame(secondAt, capturedAts[1], label)
            }
        }
    }

    @Test
    fun `invalid mixed commands restore consumed parameters and never invoke business processes`() = runBlocking {
        val fixture = groupFixture(listOf("StringId", "FirstId", "SecondId"))
        val controller = CommandMatrixController()
        val router = load(controller)
        for (useAt in listOf(false, true)) {
            val member = memberGroup("StringId", useAt)
            val invalid = listOf(
                listOf(group("给"), memberGroup("missing", useAt), group("禁言"), group("30"), group("秒")),
                listOf(group("给"), member, group("禁言"), group("不是数字"), group("秒")),
                listOf(group("给"), member, group("禁言"), group("2147483648"), group("秒")),
                listOf(group("给"), member, group("禁言"), group("30"), group("分钟")),
                listOf(group("给"), member, group("禁言"), listOf(Text("3"), At("FirstId"), Text("0")), group("秒")),
                listOf(group("把"), member, group("的"), group("12"), group("点"), group("转给")),
                listOf(group("把"), member, group("的"), group("12"), group("点"), group("转 给"), memberGroup("SecondId", useAt)),
                listOf(group("配对"), memberGroup("FirstId", useAt), memberGroup("missing", useAt), group("确认")),
                listOf(group("配对"), memberGroup("FirstId", useAt), memberGroup("SecondId", useAt), group("确认多余")),
                listOf(group("提醒"), member, group("内容"), group("hello world"), group("未结束"))
            )
            for (groups in invalid) forEachMessageVariant(groups) { body, label ->
                fixture.recorder.sent.clear()
                val context = fixture.context(body)
                context["kept"] = "original"
                context.result = "previous result"
                val position = context.cursor.mark()
                val original = body.toList()
                assertFalse(router(context), "$useAt / $label")
                assertEquals(position, context.cursor.mark(), label)
                assertEquals("original", context["kept"], label)
                assertEquals("previous result", context.result, label)
                for (name in listOf("member", "seconds", "from", "to", "amount", "left", "right", "text")) {
                    assertNull(context[name], "$name / $label")
                }
                assertNull(context.runtimeError, label)
                assertNull(context.actionInvoker, label)
                assertEquals(emptyList(), replies(context), label)
                assertEquals(0, controller.calls, label)
                assertOriginalItems(original, body, label)
            }
        }
    }

    @Test
    fun `unknown member conversion rolls back for an At consumer`() = runBlocking {
        val fixture = groupFixture()
        val member = BanController()
        val router = load(member, RawAtController())
        val context = fixture.context(MessageBody().append(Text("给")).append(At("missing")).append(Text("禁言60秒")))
        assertTrue(router(context))
        assertEquals(listOf("raw:missing:60"), replies(context))
        assertNull(member.member)
    }

    @Test
    fun `image binding retains the original MessageItem instance`() = runBlocking {
        val controller = ImageController()
        val router = load(controller)
        val image = Image("image-id", "test", "")
        val body = MessageBody().append(Text("检查")).append(image).append(Text("结束"))
        val context = context(body)
        assertTrue(router(context))
        assertSame(image, controller.image)
        assertSame(image, body[1])
        assertEquals(listOf("image-id"), replies(context))
    }

    @Test
    fun `message body aggregation preserves mixed content and the original chain`() = runBlocking {
        val controller = BodyController()
        val router = load(controller)
        val at = At("42")
        val image = Image("image-id", "test", "")
        val body = MessageBody().append(Text("转发hello world")).append(at).append(image).append(Text("完成"))
        val context = context(body)
        assertTrue(router(context))
        val captured = controller.content!!
        assertEquals("hello world", (captured[0] as Text).text)
        assertSame(at, captured[1])
        assertSame(image, captured[2])
        assertEquals(4, body.size)
        assertEquals("转发hello world", (body[0] as Text).text)
        assertEquals("完成", (body[3] as Text).text)
    }

    @Test
    fun `multiple At items aggregate into a typed member array`() = runBlocking {
        val fixture = groupFixture()
        val controller = MembersController()
        val router = load(controller)
        val body = MessageBody().append(Text("点名")).append(At("42")).append(Text(" ")).append(At("43"))
        val context = fixture.context(body)
        assertTrue(router(context))
        assertEquals(listOf("42,43"), replies(context))
        assertSame(fixture.members["42"], controller.members!![0])
        assertSame(fixture.members["43"], controller.members!![1])
    }

    @Test
    fun `MessageItem arrays retain whitespace items and mixed rich objects`() = runBlocking {
        val controller = ItemsController()
        val router = load(controller)
        val at = At("42")
        val gap = Text(" ")
        val image = Image("image-id", "test", "")
        val body = MessageBody().append(Text("集合")).append(at).append(gap).append(image)
        assertTrue(router(context(body)))
        assertEquals(3, controller.items!!.size)
        assertSame(at, controller.items!![0])
        assertSame(gap, controller.items!![1])
        assertSame(image, controller.items!![2])
    }

    @Test
    fun `map values use generic numeric and enum conversion without input tokenization`() = runBlocking {
        val controller = MapsController()
        val router = load(controller)
        assertTrue(router(context("Map匹配体质40精神60攻击20")))
        assertEquals(mapOf("体质" to 40, "精神" to 60, "攻击" to 20), controller.stats)
        assertTrue(router(context("录入成员张三男李四女王五男")))
        assertEquals(mapOf("张三" to Sex.男, "李四" to Sex.女, "王五" to Sex.男), controller.people)
    }

    @Test
    fun `Path and Action both retain consumption group declarations`() = runBlocking {
        val router = load(PrefixedController())
        for (input in listOf("管理张三设置3次", "管理 张三 设置 3次", "管理张 三设置3次")) {
            val context = context(input)
            assertTrue(router(context))
            val name = if (input.contains("张 三")) "张 三" else "张三"
            assertEquals(listOf("$name:3"), replies(context))
        }
        assertFalse(router(context("设置3次")))
    }

    @Test
    fun `slash and escaped braces are ordinary literal content`() = runBlocking {
        val router = load(LiteralController())
        val slash = context("/ping")
        assertTrue(router(slash))
        assertFalse(router(context("ping")))
        val braces = context("{12}")
        assertTrue(router(braces))
        assertEquals(listOf("12"), replies(braces))
    }

    @Test
    fun `registered domain conversion participates in the same backtracking engine`() = runBlocking {
        val controller = MoneyController()
        val converter = BotParameterConverter()
        converter.register(Money::class.java) { body, _ ->
            if (body.any { it !is Text }) null
            else body.joinToString("") { (it as Text).text }.toBigDecimalOrNull()?.let(::Money)
        }
        val loader = BotControllerLoader(TestDiContext(controller), converter)
        loader.load(listOf(item(controller.javaClass)))
        val context = context("价格12.50元")
        assertTrue(loader.rootRouter!!(context))
        assertEquals(BigDecimal("12.50"), controller.money?.amount)
        assertFalse(loader.rootRouter!!(context("价格未知元")))
    }

    @Test
    fun `custom dynamic consumer failure restores a consumed rich item`() = runBlocking {
        val router = load(RawImageController())
        val consumer = RouterMatcher { context ->
            if (context.cursor.currentItem !is Image) false
            else {
                context["leak"] = context.cursor.currentItem
                context.cursor.nextItem()
                context.cursor.isNext("missing")
            }
        }
        router.router.subRouters.add(0, BotRouter(consumer))
        val image = Image("image-id", "test", "")
        val context = context(MessageBody().append(image).append(Text("完成")))
        assertTrue(router(context))
        assertNull(context["leak"])
        assertEquals(listOf("image-id"), replies(context))
    }

    @BotController class AdjacentController {
        @BotAction("数值{seconds}结束") fun value(seconds: Int) = seconds.toString()
    }
    @BotController class SpacedController {
        @BotAction("禁言 {seconds} 秒") fun ban(seconds: Int) = seconds.toString()
    }
    @BotController class BacktrackingController {
        var beforeCalls = 0
        var catchCalls = 0
        @Before fun before() { beforeCalls++ }
        @Catch(error = Exception::class) fun catch(error: Exception) { catchCalls++ }
        @BotAction("说{text}再加{number}") fun say(text: String, number: Int) = "$text:$number"
    }
    @BotController class EchoController {
        @BotAction("说 {text}") fun say(text: String) = text
    }
    @BotController class NumericController {
        var beforeCalls = 0
        var catchCalls = 0
        @Before fun before() { beforeCalls++ }
        @Catch(error = Exception::class) fun catch(error: Exception) { catchCalls++ }
        @BotAction("值{value}") fun value(value: Int) = "number:$value"
    }
    @BotController class TextController {
        @BotAction("值{value}") fun value(value: String) = "text:$value"
    }
    @BotController class SkippingPartitionController {
        var calls = 0
        @BotAction("跳过 {left} {right}") fun skip(left: Int, right: Int): Any { calls++; return SkipMe }
    }
    @BotController class PartitionFallbackController {
        @BotAction("跳过 {text}") fun fallback(text: String) = text
    }
    enum class Sex { 男, 女 }
    @BotController class StrictTypesController {
        var calls = 0
        @Before fun before() { calls++ }
        @BotAction("byte{value}") fun byte(value: Byte) = value.toString()
        @BotAction("boolean{value}") fun boolean(value: Boolean) = value.toString()
        @BotAction("enum{value}") fun enum(value: Sex) = value.name
    }
    @BotController class BanController {
        var member: GroupMember? = null
        @BotAction("给 {member} 禁言 {seconds} 秒")
        fun ban(member: GroupMember, seconds: Int): String { this.member = member; return "${member.id}:$seconds" }
    }
    @BotController class RawAtController {
        @BotAction("给 {member} 禁言 {seconds} 秒") fun ban(member: At, seconds: Int) = "raw:${member.target}:$seconds"
    }
    @BotController class CommandMatrixController {
        var calls = 0
        @Before fun before() { calls++ }
        @BotAction("给 {member} 禁言 {seconds} 秒")
        fun ban(member: GroupMember, seconds: Int) = "${member.id}:$seconds"
        @BotAction("把 {from} 的 {amount} 点 转给 {to}")
        fun transfer(from: GroupMember, amount: Int, to: GroupMember) = "${from.id}->${to.id}:$amount"
        @BotAction("提醒 {member} 内容 {text} 完成")
        fun remind(member: GroupMember, text: String) = "${member.id}:$text"
        @BotAction("配对 {left} {right} 确认")
        fun pair(left: GroupMember, right: GroupMember) = "${left.id}+${right.id}"
        @BotAction("转发 {member} 内容 {payload} 完成")
        fun forward(member: GroupMember, payload: MessageBody) = "forward:${member.id}"
    }
    @BotController @Path("管理 {member}") class PrefixedMatrixController {
        var calls = 0
        @Before fun before() { calls++ }
        @BotAction("设置 昵称 {nickname} 确认")
        fun rename(member: GroupMember, nickname: String) = "${member.id}:$nickname"
    }
    @BotController class ImageController {
        var image: Image? = null
        @BotAction("检查{value}结束") fun inspect(value: Image): String { image = value; return value.id }
    }
    @BotController class RawImageController {
        @BotAction("{value}完成") fun inspect(value: Image) = value.id
    }
    @BotController class BodyController {
        var content: MessageBody? = null
        @BotAction("转发{value}完成") fun forward(value: MessageBody) { content = value }
    }
    @BotController class MembersController {
        var members: Array<GroupMember>? = null
        @BotAction("点名 {value}") fun members(value: Array<GroupMember>): String {
            members = value; return value.joinToString(",") { it.id }
        }
    }
    @BotController class ItemsController {
        var items: Array<MessageItem>? = null
        @BotAction("集合 {value}") fun items(value: Array<MessageItem>) { items = value }
    }
    @BotController class MapsController {
        var stats: Map<String, Int>? = null
        var people: Map<String, Sex>? = null
        @BotAction("Map匹配{value}") fun stats(value: Map<String, Int>) { stats = value }
        @BotAction("录入成员{value}") fun people(value: Map<String, Sex>) { people = value }
    }
    @BotController @Path("管理 {name}") class PrefixedController {
        @BotAction("设置 {count}次") fun set(name: String, count: Int) = "$name:$count"
    }
    @BotController class LiteralController {
        @BotAction("/ping") fun ping() = "pong"
        @BotAction("\\{{number}\\}") fun number(number: Int) = number.toString()
    }
    data class Money(val amount: BigDecimal)
    @BotController class MoneyController {
        var money: Money? = null
        @BotAction("价格{value}元") fun price(value: Money) { money = value }
    }

    private enum class TextLayout { COALESCED, PER_GROUP, PER_CHARACTER }

    private fun group(text: String): List<MessageItem> = listOf(Text(text))
    private fun memberGroup(id: String, useAt: Boolean): List<MessageItem> = listOf(if (useAt) At(id) else Text(id))

    /** Exhaustive short gaps plus reproducible long gaps; input is never tokenized. */
    private fun gapPlans(boundaries: Int): Sequence<List<Int>> = sequence {
        val lengths = listOf(0, 1, 4)
        val combinations = (0 until boundaries).fold(1) { total, _ -> total * lengths.size }
        for (encoded in 0 until combinations) {
            var remaining = encoded
            yield(List(boundaries) {
                lengths[remaining % lengths.size].also { remaining /= lengths.size }
            })
        }
        yield(List(boundaries) { 64 })
        val random = Random(20261013 + boundaries)
        repeat(24) { yield(List(boundaries) { random.nextInt(65) }) }
    }

    private suspend fun forEachMessageVariant(
        groups: List<List<MessageItem>>,
        check: suspend (MessageBody, String) -> Unit
    ) {
        val boundaries = groups.size - 1
        for (gaps in gapPlans(boundaries)) for (layout in TextLayout.entries) {
            check(renderMessage(groups, gaps, layout), "$layout / gaps=$gaps")
        }
        // Very long individual gaps also exercise cursor jumps without a huge Cartesian product.
        for (boundary in 0 until boundaries) {
            val gaps = List(boundaries) { if (it == boundary) 4096 else 0 }
            check(renderMessage(groups, gaps, TextLayout.COALESCED), "long boundary=$boundary")
        }
    }

    private fun renderMessage(groups: List<List<MessageItem>>, gaps: List<Int>, layout: TextLayout): MessageBody {
        val body = MessageBody()
        val pending = StringBuilder()
        fun flush() {
            if (pending.isNotEmpty()) {
                body.append(Text(pending.toString()))
                pending.clear()
            }
        }
        fun append(item: MessageItem) {
            if (item !is Text) {
                flush()
                body.append(item)
            } else when (layout) {
                TextLayout.COALESCED -> pending.append(item.text)
                TextLayout.PER_GROUP -> body.append(item)
                TextLayout.PER_CHARACTER -> item.text.codePoints().forEach { codePoint ->
                    body.append(Text(String(Character.toChars(codePoint))))
                }
            }
        }
        for ((index, items) in groups.withIndex()) {
            if (index > 0) append(Text(" ".repeat(gaps[index - 1])))
            items.forEach(::append)
        }
        flush()
        return body
    }

    private suspend fun assertMatrixMatch(
        fixture: GroupFixture,
        router: BotRootRouter,
        body: MessageBody,
        expectedReply: String,
        label: String,
        calls: () -> Int,
        expectedValues: Map<String, Any>
    ): BotActionContext {
        fixture.recorder.sent.clear()
        val context = fixture.context(body)
        val original = body.toList()
        val previousCalls = calls()
        assertTrue(router(context), "$expectedReply / $label")
        assertTrue(context.cursor.end, label)
        for ((name, value) in expectedValues) {
            if (value is GroupMember) assertSame(value, context[name], "$name / $label")
            else assertEquals(value, context[name], "$name / $label")
        }
        assertEquals(previousCalls + 1, calls(), label)
        assertEquals(listOf(expectedReply), replies(context), label)
        assertNull(context.runtimeError, label)
        assertOriginalItems(original, body, label)
        return context
    }

    private fun assertOriginalItems(original: List<MessageItem>, body: MessageBody, label: String) {
        assertEquals(original.size, body.size, label)
        for ((index, item) in original.withIndex()) assertSame(item, body[index], "$index / $label")
    }

    private fun load(vararg controllers: Any): BotRootRouter =
        BotControllerLoader(TestDiContext(*controllers)).also { loader ->
            loader.load(controllers.map { item(it.javaClass) })
        }.rootRouter!!

    private fun item(type: Class<*>): LoadItem {
        val annotation = type.annotations.first { it.annotationClass.java.getAnnotation(LoadBy::class.java) != null }
        return LoadItem(type, annotation.annotationClass.java, annotation)
    }

    private fun context(text: String) = context(MessageBody().append(Text(text)))
    private fun context(body: MessageBody): BotActionContext {
        val recorder = RecordingFriend()
        return BotActionContext(recorder.bot, MessageChannel.Friend, recorder, recorder, body.toMessage())
            .also { it["recorder"] = recorder }
    }
    private fun replies(context: BotActionContext) = (context["recorder"] as RecordingFriend).sent.map {
        it.body.joinToString("") { item -> (item as Text).text }
    }

    private data class GroupFixture(val recorder: RecordingFriend, val members: Map<String, GroupMember>, val group: Group) {
        fun context(body: MessageBody): BotActionContext =
            BotActionContext(recorder.bot, MessageChannel.Group, recorder, group, body.toMessage())
                .also { it["recorder"] = recorder }
    }

    private fun groupFixture(ids: List<String> = listOf("42", "43")): GroupFixture {
        val recorder = RecordingFriend()
        val properties = ids.associateWith { id ->
            mutableMapOf<String, Any?>("getId" to id, "getBot" to recorder.bot)
        }
        val members = properties.mapValues { (_, values) -> stub(GroupMember::class.java, values, recorder) }
        val group = stub(Group::class.java, mapOf("getId" to "group", "getMembers" to Members(members), "getBot" to recorder.bot), recorder)
        properties.values.forEach { it["getGroup"] = group }
        return GroupFixture(recorder, members, group)
    }

    private fun <T> stub(type: Class<T>, values: Map<String, Any?>, recorder: RecordingFriend): T =
        type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when (method.name) {
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.get(0)
                "toString" -> "Stub(${type.simpleName})"
                "sendMessage" -> recorder.sendMessage(args!![0] as Message)
                else -> if (method.name in values) values[method.name] else error("Unexpected call: ${method.name}")
            }
        })

    private class Members(private val byId: Map<String, GroupMember>) : AccountList<Long, GroupMember>, Map<String, GroupMember> by byId {
        private val byPlatformId = byId.values.withIndex().associate { (index, member) -> (index + 1).toLong() to member }
        override val platformIds: Set<Long> = byPlatformId.keys
        override val platformEntries: Set<Map.Entry<Long, GroupMember>> =
            byPlatformId.map { AbstractMap.SimpleImmutableEntry(it.key, it.value) }.toSet()
        override fun getByPlatformId(platformId: Long): GroupMember? = byPlatformId[platformId]
    }

    private class TestDiContext(vararg instances: Any) : DiContext {
        private val beans: MutableMap<Class<*>, Any> = HashMap<Class<*>, Any>().apply { instances.forEach { put(it.javaClass, it) } }
        override fun <T> getBean(clazz: Class<T>, instanceName: String): T? = beans[clazz]?.let(clazz::cast)
        override fun <T> putBean(clazz: Class<T>, instanceName: String, instance: T): T { beans[clazz] = instance as Any; return instance }
        override fun <T> newBean(clazz: Class<T>): T = error("Unexpected construction: ${clazz.name}")
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
            return object : MessageSource { override val id = sent.size.toString(); override suspend fun recall() {} }
        }
        override fun uploadImage(imageFile: File): Image = error("Unused in controller tests")
        override fun sendFile(file: File) {}
        override fun delete() {}
    }
}
