# 消息消费声明与参数处理器

`@Path` 与 Action 声明编译为同一套动态消费节点。声明中的空白划分消费组，消息在这些组之间可以没有空白，也可以有任意数量的空白。
原 MessageItem 链保持完整；相邻 Text 可以跨项消费，At、Image 等对象保留原实例。

| 声明 | 消费行为 |
| --- | --- |
| `{name}` | 必选参数，根据 Action 方法参数类型转换 |
| `{name?}` 或 `{name}?` | 可选参数，先尝试有值，再尝试零消费缺省 |
| `[内容]` | 整体可跳过，可以包含多个消费组与参数 |
| `(内容)` | 以固定格式作为一次整体消费；括号本身不匹配消息字符 |
| `{items}+`、`(格式)+` | 重复一次或多次，捕获参数按元素类型解析后聚合 |
| `{items}*`、`(格式)*` | 重复零次或多次；零次得到空的对应类型容器 |
| `\[`、`\(`、`\?` 等 | 匹配语法符号本身；也支持转义大括号、反斜杠和空白 |

## 可选参数与可跳过格式

```kotlin
@BotAction("给 {member} 禁言 [{seconds} 秒] [理由 {reason}]")
fun ban(member: GroupMember, seconds: Int = 60, reason: String?) {
    // 使用 seconds 和 reason
}
```

同一声明支持以下消息，其中 `@member` 表示真实 At 消息项：

| 消息 | seconds | reason |
| --- | --- | --- |
| `给@member禁言` | 60 | null |
| `给StringId禁言30秒` | 30 | null |
| `给@member禁言理由刷 屏` | 60 | `刷 屏` |
| `给StringId禁言 30 秒 理由刷屏` | 30 | `刷屏` |

`[{seconds} 秒]` 把数字与单位作为整体；只提供数字或只提供“秒”都会匹配失败。
`{seconds?} 秒` 则只允许数字缺省，单位仍然必选。
可选捕获对应的 Kotlin 参数必须可空或具有默认值；Java 参数使用 Rain `@Nullable`。
默认值、可空类型本身不会自动改变声明的必选性，消息消费的可选性由 `?` 或 `[]` 明确表达。

缺省参数由 `context.isAbsent(name)` 区分，并在调用阶段使用默认值或 null；它不会落入联系人注入并误取发送者。
转换失败与缺省分开处理；跳过分支不丢弃非法输入，因此 `禁言abc秒` 无法通过。
有值分支优先尝试，后续失败后才恢复检查点并尝试缺省；前面的参数也可以直接接上可选组之后的下一组。

## 固定格式与数组聚合

```kotlin
@BotAction("录入 ({values} 分)+ 完成")
fun scores(values: IntArray) {
    // 录入10分20分30分完成 -> [10, 20, 30]
}

@BotAction("点名 {members}+ 确认")
fun named(members: List<GroupMember>) {
    // 点名@FirstIdSecondId确认：第一个成员是 At，第二个是 Text ID
}

@BotAction("登记 (姓名={names},分数={scores};)+ 完成")
fun record(names: List<String>, scores: IntArray) {
    // 登记姓名=张 三,分数=10;姓名=李四,分数=20;完成
    // names = ["张 三", "李四"], scores = [10, 20]
}
```

重复体中的每个捕获按容器的元素类型解析，支持 JVM 数组（含原生类型数组）、List、Collection 与 Set。
同一格式中的多个参数可以聚合为对应的多组数组或集合；字符串内部空格保留。
每两次重复之间允许零个或多个空白，每次重复都必须消费完整格式。
重复优先继续消费，后续失败时可以减少重复次数，或重试某一项的另一个消费范围。
通过显式栈推进重复，数组长度不会增加 JVM 调用栈深度。

## 按类型注册格式处理器

`BotParameterConverter.register` 保留按已捕获范围转换的接口。
`registerPattern` 与 `registerHandler` 则让类型处理器直接决定一次参数消费的长度；在重复声明中自动用于容器元素类型。

```kotlin
data class Point(val x: Int, val y: Int)

val converter = BotParameterConverter()
converter.registerPattern(Point::class.java, Regex("\\(([+-]?\\d+),([+-]?\\d+)\\)")) { match ->
    val x = match.groupValues[1].toIntOrNull()
    val y = match.groupValues[2].toIntOrNull()
    if (x == null || y == null) null else Point(x, y)
}

@BotAction("画 {points}+ 完成")
fun draw(points: List<Point>) {
    // 画(1,2) (3,4)完成 -> [Point(1, 2), Point(3, 4)]
}
```

将 converter 注册到 DiContext 后 Loader 会自动取得它，也可以显式调用 `BotControllerLoader(di, converter)`。
注册按目标类型匹配；原生标量类型与包装类型统一处理。
`registerPattern` 只匹配当前位置的一个文本前缀，可以跨相邻 Text 项；遇到富消息项会停止收集文本。
格式存在多个合法长度或需要消费 At、Image 等消息项时，使用 `registerHandler` 返回多个候选：

```kotlin
data class MemberRef(val id: String, val original: At)

converter.registerHandler(MemberRef::class.java) { input ->
    sequence {
        val at = input.cursor.currentItem as? At ?: return@sequence
        input.cursor.nextItem()
        yield(BotParameterCandidate(input.cursor.mark(), MemberRef(at.target, at)))
    }
}
```

`input` 提供参数名、含泛型信息的 RelType、上下文及独立游标。处理器通过独立游标读取消息，并将候选结束位置和类型值交给引擎；上下文用于只读查询。
引擎负责参数绑定、下级验证、候选重试及状态回退。候选必须向前消费至少一项或一个文本字符，并返回与目标类型兼容的值。
注册了处理器的类型以处理器的结果为准；没有合法格式时直接匹配失败。

## 声明校验与回退

同一个参数名在声明中只出现一次，重复采集通过 `*`、`+` 表达。
重复体必须具有消费内容；可选捕获应放在重复格式外，避免一项缺字段导致平行数组错位。
当前不支持捕获的嵌套重复；复杂单项结构可以使用类型处理器，再在外层重复。
字面括号、方括号与量词符号使用反斜杠转义。

所有分支失败后恢复游标、捕获值、结果、错误和调用器状态，原消息链不变。
完整匹配前不会执行业务流程；同一 Action 在一次分发中返回 SkipMe 后不会因新的参数划分重复调用。

测试覆盖可选组的 3,600 种 At/Text、空格及文本碎片组合，512 项格式数组、数组回退、候选重试、Java 可空与泛型集合，以及此前的混合消息消费矩阵。
