package yuq.controller

import kotlinx.coroutines.runBlocking
import rain.api.annotation.LoadBy
import rain.api.di.DiContext
import rain.api.loader.LoadItem
import rain.controller.annotation.Before
import rain.controller.annotation.Path
import yuq.Bot
import yuq.annotation.BotAction
import yuq.annotation.BotController
import yuq.contact.AccountList
import yuq.contact.ContactSession
import yuq.contact.Friend
import yuq.contact.Group
import yuq.contact.GroupMember
import yuq.controller.router.BotRootRouter
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
import kotlin.test.*

class OptionalParameterParsingTest {
    @Test
    fun `optional duration and reason work with every At Text and short gap combination`() = runBlocking {
        val fixture = Fixture()
        val controller = OptionalController()
        val router = load(controller)
        for (mask in 0 until 4) for (useAt in listOf(false, true)) {
            val parts = mutableListOf<MessageItem>(Text("给"), if (useAt) At("FirstId") else Text("FirstId"), Text("禁言"))
            if (mask and 1 != 0) parts.addAll(listOf(Text("30"), Text("秒")))
            if (mask and 2 != 0) parts.addAll(listOf(Text("理由"), Text("刷 屏")))
            val combinations = (1 until parts.size).fold(1) { total, _ -> total * 3 }
            for (encoded in 0 until combinations) for (fragmented in listOf(false, true)) {
                var remaining = encoded
                val gaps = List(parts.size - 1) {
                    listOf(0, 1, 5)[remaining % 3].also { remaining /= 3 }
                }
                val body = message(parts, gaps, fragmented)
                val seconds = if (mask and 1 != 0) 30 else 60
                val reason = if (mask and 2 != 0) "刷 屏" else null
                val before = controller.calls
                val context = assertMatch(fixture, router, body, "FirstId:$seconds:$reason", "$mask/$useAt/$gaps/$fragmented")
                assertSame(fixture.members.getValue("FirstId"), context["member"])
                assertEquals(mask and 1 == 0, context.isAbsent("seconds"))
                assertEquals(mask and 2 == 0, context.isAbsent("reason"))
                if (mask and 1 != 0) assertEquals(30, context["seconds"])
                assertEquals(reason, context["reason"])
                assertEquals(before + 1, controller.calls)
            }
        }
    }

    @Test
    fun `partial optional formats and malformed values cannot disappear`() = runBlocking {
        val fixture = Fixture()
        val controller = OptionalController()
        val router = load(controller)
        for (suffix in listOf("30", "秒", "abc秒", "2147483648秒", "30分钟", "理由", "未知")) {
            val body = MessageBody().append(Text("给")).append(At("FirstId")).append(Text("禁言$suffix"))
            assertRejected(fixture, router, body, listOf("member", "seconds", "reason"))
        }
        assertEquals(0, controller.calls)
    }

    @Test
    fun `optional parameter uses a Kotlin default while nullable contact omission blocks sender injection`() = runBlocking {
        val fixture = Fixture()
        val router = load(OptionalController())
        val default = assertMatch(fixture, router, text("设置完成"), "7")
        assertTrue(default.isAbsent("count"))
        assertMatch(fixture, router, text("设置 12 完成", true), "12")
        assertRejected(fixture, router, text("设置abc完成"), listOf("count"))
        val absent = assertMatch(fixture, router, text("查询确认"), "none")
        assertSame(fixture.members.getValue("FirstId"), absent.sender)
        assertTrue(absent.contains("target"))
        assertTrue(absent.isAbsent("target"))
        assertNull(absent["target"])
        val present = assertMatch(fixture, router,
            MessageBody().append(Text("查询 ")).append(At("SecondId")).append(Text(" 确认")), "SecondId")
        assertSame(fixture.members.getValue("SecondId"), present["target"])
        assertFalse(present.isAbsent("target"))
    }

    @Test
    fun `parameter before an optional group can start the later required group directly`() = runBlocking {
        val fixture = Fixture()
        val router = load(OptionalController())
        val absent = assertMatch(fixture, router, text("找Alice确认", true), "Alice:null")
        assertEquals("Alice", absent["name"])
        assertTrue(absent.isAbsent("note"))
        val present = assertMatch(fixture, router, text("找 Alice 备注 hello world 确认", true), "Alice:hello world")
        assertEquals("hello world", present["note"])
    }

    @Test
    fun `optional presence rolls back when only the absent branch completes the remainder`(): Unit = runBlocking {
        val fixture = Fixture()
        val controller = OptionalController()
        val router = load(controller)
        val context = assertMatch(fixture, router, text("解析1后", true), "null:1后")
        assertTrue(context.isAbsent("number"))
        assertEquals("1后", context["tail"])
        assertEquals(1, controller.calls)
        assertMatch(fixture, router, text("解析2后正文"), "2:正文")
    }

    @Test
    fun `optional literal prefixes and nested optional groups share the same rollback protocol`() = runBlocking {
        val fixture = Fixture()
        val router = load(OptionalController())
        for (input in listOf("执行确认", "请执行确认", "请 执行 用 理由测试 确认", "执行用详细理由测试确认")) {
            val expected = if ("测试" in input) "测试" else "null"
            assertMatch(fixture, router, text(input, true), expected)
        }
        assertRejected(fixture, router, text("执行用详细确认"), listOf("why"))
    }

    @Test
    fun `optional groups in Path and Action both resolve absent parameters`() = runBlocking {
        val fixture = Fixture()
        val router = load(PrefixedOptionalController())
        assertMatch(fixture, router, text("管理完成", true), "none:9")
        val context = assertMatch(fixture, router, MessageBody().append(Text("管理 ")).append(At("SecondId"))
            .append(Text(" 设置 12 完成")), "SecondId:12")
        assertSame(fixture.members.getValue("SecondId"), context["member"])
    }

    @Test
    fun `empty whitespace groups keep trailing separators out of numeric and text parameters`() = runBlocking {
        val fixture = Fixture()
        val router = load(OptionalController())
        for (gap in listOf("", " ", " ".repeat(64))) {
            val count = assertMatch(fixture, router, text("尾参30$gap", true), "30")
            assertEquals(30, count["count"])
            val content = assertMatch(fixture, router, text("正文hello world${gap}!", true), "hello world")
            assertEquals("hello world", content["value"])
        }
    }

    @Test
    fun `formatted repetition collects whole numeric units into a primitive array`() = runBlocking {
        val fixture = Fixture()
        val controller = RepeatedController()
        val router = load(controller)
        for (gap in listOf("", " ", "     ", "\t\n", " ".repeat(64))) {
            for (fragmented in listOf(false, true)) {
                val context = assertMatch(fixture, router, text("录入${gap}10${gap}分${gap}20${gap}分${gap}30${gap}分${gap}完成", fragmented), "10,20,30")
                assertContentEquals(intArrayOf(10, 20, 30), context["values"] as IntArray)
            }
        }
        val long = (0 until 512).joinToString("") { "${it}分" }
        val context = assertMatch(fixture, router, text("录入${long}完成"), (0 until 512).joinToString(","))
        assertContentEquals(IntArray(512) { it }, context["values"] as IntArray)
    }

    @Test
    fun `zero repetitions produce empty typed arrays and plus rejects empty input`() = runBlocking {
        val fixture = Fixture()
        val router = load(RepeatedController())
        val empty = assertMatch(fixture, router, text("收集 完成", true), "0")
        assertContentEquals(intArrayOf(), empty["values"] as IntArray)
        assertFalse(empty.isAbsent("values"))
        val members = assertMatch(fixture, router, text("成员确认"), "0")
        assertEquals(0, (members["members"] as Array<*>).size)
        assertRejected(fixture, router, text("录入完成"), listOf("values"))
        assertRejected(fixture, router, text("点名确认"), listOf("members"))
    }

    @Test
    fun `repeated members support mixed At and Text with zero or many spaces between elements`() = runBlocking {
        val fixture = Fixture()
        val router = load(RepeatedController())
        for (firstAt in listOf(false, true)) for (secondAt in listOf(false, true)) {
            for (gap in listOf(0, 1, 5, 64)) for (fragmented in listOf(false, true)) {
                val parts = listOf(Text("点名"), if (firstAt) At("FirstId") else Text("FirstId"),
                    if (secondAt) At("SecondId") else Text("SecondId"), Text("确认"))
                val context = assertMatch(fixture, router, message(parts, List(3) { gap }, fragmented), "FirstId,SecondId")
                val captured = context["members"] as List<*>
                assertEquals(2, captured.size)
                assertSame(fixture.members.getValue("FirstId"), captured[0])
                assertSame(fixture.members.getValue("SecondId"), captured[1])
            }
        }
    }

    @Test
    fun `one repeated format collects parallel fields and preserves spaces inside values`() = runBlocking {
        val fixture = Fixture()
        val router = load(RepeatedController())
        val context = assertMatch(fixture, router,
            text("登记姓名=张 三,分数=10;   姓名=李四,分数=20; 完成", true), "张 三:10,李四:20")
        assertEquals(listOf("张 三", "李四"), context["names"])
        assertContentEquals(intArrayOf(10, 20), context["scores"] as IntArray)
        val labels = assertMatch(fixture, router, text("标签甲;乙;甲;完成"), "甲,乙")
        assertEquals(setOf(Label.甲, Label.乙), labels["labels"])
    }

    @Test
    fun `invalid later repetitions restore all accumulated bindings before falling back`() = runBlocking {
        val fixture = Fixture()
        val controller = RepeatedController()
        val router = load(controller)
        for (input in listOf("录入10分20完成", "录入10分oops分完成", "录入10分20分未知", "登记姓名=张三,分数=10;姓名=李四,分数=oops;完成")) {
            assertRejected(fixture, router, text(input, true), listOf("values", "names", "scores"))
        }
        val mixed = MessageBody().append(Text("录入10分2")).append(At("FirstId")).append(Text("0分完成"))
        assertRejected(fixture, router, mixed, listOf("values"))
        assertEquals(0, controller.calls)
    }

    @Test
    fun `repetition gives a consumed member back when the later required parameter needs it`() = runBlocking {
        val fixture = Fixture()
        val router = load(RepeatedController())
        for (firstAt in listOf(false, true)) for (lastAt in listOf(false, true)) {
            val parts = listOf(Text("分配"), if (firstAt) At("FirstId") else Text("FirstId"),
                if (lastAt) At("SecondId") else Text("SecondId"), Text("确认"))
            val context = assertMatch(fixture, router, message(parts, listOf(0, 5, 0), true), "FirstId->SecondId")
            val captured = context["members"] as List<*>
            assertEquals(1, captured.size)
            assertSame(fixture.members.getValue("FirstId"), captured[0])
            assertSame(fixture.members.getValue("SecondId"), context["last"])
        }
    }

    @Test
    fun `Java nullable parameters and generic repeated members use the same parser`() = runBlocking {
        val fixture = Fixture()
        val router = load(JavaParameterParsingController())
        val absent = assertMatch(fixture, router, text("java-null确认", true), "none")
        assertTrue(absent.isAbsent("target"))
        val body = MessageBody().append(Text("java-members")).append(At("FirstId"))
            .append(Text("    SecondId 确认"))
        val present = assertMatch(fixture, router, body, "FirstId,SecondId")
        val members = present["members"] as List<*>
        assertSame(fixture.members.getValue("FirstId"), members[0])
        assertSame(fixture.members.getValue("SecondId"), members[1])
    }

    @Test
    fun `typed fixed format handler consumes fragmented text and repeated domain objects`() = runBlocking {
        val fixture = Fixture()
        val router = load(HandlerController(), handlers())
        val money = assertMatch(fixture, router, text("支付￥12.50元确认", true), "12.50")
        assertEquals(Coins(BigDecimal("12.50")), money["money"])
        for (gap in listOf("", " ", " ".repeat(64))) {
            val points = assertMatch(fixture, router, text("画(1,2)${gap}(3,4)${gap}完成", true), "1,2;3,4")
            assertEquals(listOf(Point(1, 2), Point(3, 4)), points["points"])
        }
        assertRejected(fixture, router, text("支付12.50确认"), listOf("money"))
        assertRejected(fixture, router, text("画(1,2)(3,x)完成"), listOf("points"))
    }

    @Test
    fun `typed handler retains rich item identity and can also parse a text format`() = runBlocking {
        val fixture = Fixture()
        val router = load(HandlerController(), handlers())
        val at = At("SecondId")
        val rich = assertMatch(fixture, router, MessageBody().append(Text("识别 ")).append(at).append(Text(" 确认")), "SecondId")
        assertSame(at, (rich["reference"] as MemberRef).original)
        val plain = assertMatch(fixture, router, text("识别id:FirstId确认", true), "FirstId")
        assertEquals(MemberRef("FirstId", null), plain["reference"])
    }

    @Test
    fun `handler can yield another end when an earlier candidate cannot start the remainder`() = runBlocking {
        val fixture = Fixture()
        val converter = handlers()
        converter.registerHandler(Word::class.java) { input ->
            sequence {
                if (input.cursor.isNext("ab")) {
                    yield(BotParameterCandidate(input.cursor.mark(), Word("ab")))
                    if (input.cursor.isNext("cd")) yield(BotParameterCandidate(input.cursor.mark(), Word("abcd")))
                }
            }
        }
        val context = assertMatch(fixture, load(HandlerController(), converter), text("读取abcd结束", true), "abcd")
        assertEquals(Word("abcd"), context["word"])
    }

    @Test
    fun `repetition retries a longer handler candidate when the shorter element cannot complete the suffix`() = runBlocking {
        val fixture = Fixture()
        val converter = handlers()
        converter.registerHandler(Word::class.java) { input ->
            sequence {
                if (input.cursor.isNext("a")) {
                    yield(BotParameterCandidate(input.cursor.mark(), Word("a")))
                    if (input.cursor.isNext("b")) yield(BotParameterCandidate(input.cursor.mark(), Word("ab")))
                }
            }
        }
        val context = assertMatch(fixture, load(HandlerController(), converter), text("切分ab结束", true), "ab")
        assertEquals(listOf(Word("ab")), context["words"])
    }

    @Test
    fun `zero width handler candidate is rejected without leaking context state`() = runBlocking {
        val fixture = Fixture()
        val converter = handlers()
        converter.registerHandler(Word::class.java) { input ->
            input.context["leak"] = "changed"
            sequenceOf(BotParameterCandidate(input.start, Word("")))
        }
        val router = load(HandlerController(), converter)
        val context = fixture.context(text("读取abcd结束"))
        val position = context.cursor.mark()
        assertFailsWith<IllegalArgumentException> { router(context) }
        assertEquals(position, context.cursor.mark())
        assertNull(context["leak"])
        assertNull(context["word"])
    }

    @Test
    fun `syntax literals can be escaped and repeated groups cannot be empty`() = runBlocking {
        val fixture = Fixture()
        val router = load(EscapedController())
        assertMatch(fixture, router, text("[12](x)*+?", true), "12")
        val loader = BotControllerLoader(TestDiContext())
        for (pattern in listOf("[未关闭", "(未关闭", "{value", "([请])*", "()+", "{same}{same}")) {
            assertFailsWith<IllegalArgumentException>(pattern) { loader.readPattern(pattern) }
        }
    }

    @Test
    fun `invalid optional and repetition bindings fail at declaration load`() {
        assertFailsWith<IllegalArgumentException> { load(InvalidOptionalController()) }
        assertFailsWith<IllegalArgumentException> { load(InvalidRepeatedController()) }
    }

    @BotController class OptionalController {
        var calls = 0
        @Before fun before() { calls++ }
        @BotAction("给 {member} 禁言 [{seconds} 秒] [理由 {reason}]")
        fun ban(member: GroupMember, seconds: Int = 60, reason: String?) = "${member.id}:$seconds:$reason"
        @BotAction("设置 {count?} 完成") fun set(count: Int = 7) = count.toString()
        @BotAction("查询 {target?} 确认") fun query(target: GroupMember?) = target?.id ?: "none"
        @BotAction("找 {name} [备注 {note}] 确认") fun find(name: String, note: String?) = "$name:$note"
        @BotAction("解析 [{number} 后] {tail}") fun parse(number: Int?, tail: String) = "$number:$tail"
        @BotAction("[请] 执行 [用 [详细] 理由 {why}] 确认") fun execute(why: String?) = why.toString()
        @BotAction("尾参 {count} ") fun trailing(count: Int) = count.toString()
        @BotAction("正文 {value} [ ]!") fun content(value: String) = value
    }
    @BotController @Path("管理 [{member}]") class PrefixedOptionalController {
        @BotAction("[设置 {count}] 完成") fun manage(member: GroupMember?, count: Int = 9) = "${member?.id ?: "none"}:$count"
    }
    enum class Label { 甲, 乙 }
    @BotController class RepeatedController {
        var calls = 0
        @Before fun before() { calls++ }
        @BotAction("录入 ({values} 分)+ 完成") fun scores(values: IntArray) = values.joinToString(",")
        @BotAction("收集 ({values} 分)* 完成") fun collect(values: IntArray) = values.size.toString()
        @BotAction("点名 {members}+ 确认") fun named(members: List<GroupMember>) = members.joinToString(",") { it.id }
        @BotAction("成员 {members}* 确认") fun members(members: Array<GroupMember>) = members.size.toString()
        @BotAction("分配 {members}+ {last} 确认")
        fun assign(members: List<GroupMember>, last: GroupMember) = "${members.joinToString(",") { it.id }}->${last.id}"
        @BotAction("登记 (姓名={names},分数={scores};)+ 完成")
        fun record(names: List<String>, scores: IntArray) = names.zip(scores.toList()).joinToString(",") { "${it.first}:${it.second}" }
        @BotAction("标签 ({labels};)+ 完成") fun labels(labels: Set<Label>) = labels.joinToString(",") { it.name }
    }
    data class Point(val x: Int, val y: Int)
    data class Coins(val amount: BigDecimal)
    data class MemberRef(val id: String, val original: At?)
    data class Word(val text: String)
    @BotController class HandlerController {
        @BotAction("支付 {money} 确认") fun pay(money: Coins) = money.amount.toPlainString()
        @BotAction("画 {points}+ 完成") fun draw(points: List<Point>) = points.joinToString(";") { "${it.x},${it.y}" }
        @BotAction("识别 {reference} 确认") fun identify(reference: MemberRef) = reference.id
        @BotAction("读取 {word} 结束") fun read(word: Word) = word.text
        @BotAction("切分 {words}+ 结束") fun words(words: List<Word>) = words.joinToString("|") { it.text }
    }
    @BotController class EscapedController {
        @BotAction("\\[{number}\\]\\(x\\)\\*\\+\\?") fun escaped(number: Int) = number.toString()
    }
    @BotController class InvalidOptionalController {
        @BotAction("错误 {value?}") fun value(value: Int) = value.toString()
    }
    @BotController class InvalidRepeatedController {
        @BotAction("错误 {value}+") fun value(value: Int) = value.toString()
    }

    private fun handlers() = BotParameterConverter().apply {
        registerPattern(Coins::class.java, Regex("￥(\\d+(?:\\.\\d{2})?)元")) {
            it.groupValues[1].toBigDecimalOrNull()?.let(::Coins)
        }
        registerPattern(Point::class.java, Regex("\\(([+-]?\\d+),([+-]?\\d+)\\)")) { match ->
            val x = match.groupValues[1].toIntOrNull()
            val y = match.groupValues[2].toIntOrNull()
            if (x == null || y == null) null else Point(x, y)
        }
        registerHandler(MemberRef::class.java) { input ->
            sequence {
                val at = input.cursor.currentItem as? At
                if (at != null) {
                    input.cursor.nextItem()
                    yield(BotParameterCandidate(input.cursor.mark(), MemberRef(at.target, at)))
                } else {
                    val match = Regex("id:([A-Za-z0-9]+)").find(input.cursor.remainingText())
                        ?.takeIf { it.range.first == 0 } ?: return@sequence
                    if (input.cursor.isNext(match.value)) {
                        yield(BotParameterCandidate(input.cursor.mark(), MemberRef(match.groupValues[1], null)))
                    }
                }
            }
        }
    }

    private fun load(controller: Any, converter: BotParameterConverter = BotParameterConverter()): BotRootRouter {
        val loader = BotControllerLoader(TestDiContext(controller, converter))
        val type = controller.javaClass
        val annotation = type.annotations.first { it.annotationClass.java.getAnnotation(LoadBy::class.java) != null }
        loader.load(listOf(LoadItem(type, annotation.annotationClass.java, annotation)))
        return loader.rootRouter!!
    }

    private fun text(value: String, fragmented: Boolean = false): MessageBody = message(listOf(Text(value)), emptyList(), fragmented)
    private fun message(parts: List<MessageItem>, gaps: List<Int>, fragmented: Boolean): MessageBody {
        val body = MessageBody()
        fun append(item: MessageItem) {
            if (fragmented && item is Text) item.text.codePoints().forEach { body.append(Text(String(Character.toChars(it)))) }
            else body.append(item)
        }
        parts.forEachIndexed { index, item ->
            if (index > 0) append(Text(" ".repeat(gaps[index - 1])))
            append(item)
        }
        return body
    }

    private suspend fun assertMatch(fixture: Fixture, router: BotRootRouter, body: MessageBody, reply: String, label: String = reply): BotActionContext {
        val context = fixture.context(body)
        val original = body.toList()
        assertTrue(router(context), label)
        assertTrue(context.cursor.end, label)
        assertNull(context.runtimeError, label)
        assertEquals(listOf(reply), fixture.recorder.sent.map { it.body.joinToString("") { item -> (item as Text).text } }, label)
        assertEquals(original.size, body.size, label)
        original.forEachIndexed { index, item -> assertSame(item, body[index], label) }
        return context
    }

    private suspend fun assertRejected(fixture: Fixture, router: BotRootRouter, body: MessageBody, names: List<String>) {
        val context = fixture.context(body)
        context["kept"] = "original"
        context.result = "previous"
        val original = body.toList()
        val start = context.cursor.mark()
        assertFalse(router(context), body.joinToString())
        assertEquals(start, context.cursor.mark())
        assertEquals("original", context["kept"])
        assertEquals("previous", context.result)
        assertNull(context.runtimeError)
        assertNull(context.actionInvoker)
        names.forEach { assertFalse(context.contains(it), it) }
        assertTrue(fixture.recorder.sent.isEmpty())
        assertEquals(original.size, body.size)
        original.forEachIndexed { index, item -> assertSame(item, body[index]) }
    }

    private class Fixture {
        val recorder = RecordingFriend()
        val members: Map<String, GroupMember>
        val group: Group
        init {
            val properties = listOf("FirstId", "SecondId").associateWith { id ->
                mutableMapOf<String, Any?>("getId" to id, "getBot" to recorder.bot, "getGuid" to "test:$id")
            }
            members = properties.mapValues { (_, values) -> stub(GroupMember::class.java, values) }
            group = stub(Group::class.java, mapOf("getId" to "group", "getBot" to recorder.bot, "getMembers" to Members(members)))
            properties.values.forEach { it["getGroup"] = group }
        }
        fun context(body: MessageBody): BotActionContext {
            recorder.sent.clear()
            return BotActionContext(recorder.bot, MessageChannel.Group, members.getValue("FirstId"), group, body.toMessage())
        }
        private fun <T> stub(type: Class<T>, values: Map<String, Any?>): T =
            type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
                when (method.name) {
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.get(0)
                    "toString" -> "Stub(${type.simpleName})"
                    "sendMessage" -> recorder.sendMessage(args!![0] as Message)
                    else -> if (method.name in values) values[method.name] else error("Unexpected call: ${method.name}")
                }
            })
    }
    private class Members(private val byId: Map<String, GroupMember>) : AccountList<Long, GroupMember>, Map<String, GroupMember> by byId {
        private val byPlatformId = byId.values.withIndex().associate { (index, value) -> index.toLong() to value }
        override val platformIds = byPlatformId.keys
        override val platformEntries = byPlatformId.entries
        override fun getByPlatformId(platformId: Long) = byPlatformId[platformId]
    }
    private class TestDiContext(vararg instances: Any) : DiContext {
        private val beans: MutableMap<Class<*>, Any> = HashMap<Class<*>, Any>().apply {
            instances.forEach { put(it.javaClass, it) }
        }
        override fun <T> getBean(clazz: Class<T>, instanceName: String): T? = beans[clazz]?.let(clazz::cast)
        override fun <T> putBean(clazz: Class<T>, instanceName: String, instance: T): T { beans[clazz] = instance as Any; return instance }
        override fun <T> newBean(clazz: Class<T>): T = error("Unexpected construction: ${clazz.name}")
        override fun <T : Any> injectBean(bean: T) = bean
        override fun <T : Any> forceInjectBean(bean: T) = bean
        override fun registerClass(clazz: Class<*>) {}
    }
    private class RecordingFriend : Friend {
        override val platform = "test"
        override val runtime = "test"
        override val id = "recorder"
        override val nickname = "tester"
        override val avatar = ""
        override val guid = "test:recorder"
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
