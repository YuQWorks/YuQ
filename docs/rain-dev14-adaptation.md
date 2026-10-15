# Rain DEV14 适配

以 `yuq.*` 为后续开发的主 API，功能逐步迁移；在迁移完成前继续保留 `com.icecreamqaq.yuq.*`。

本次完成 Controller 链路对 Rain `1.0.0-DEV14` 的适配：

- `BotRouter` 与 `BotActionInvoker` 分开，根路由和 Controller 流程分别声明路由、调用器类型。
- 使用 `ActionCreator` 创建调用器，交由 Rain 组装、排序和过滤 Before / After / Catch 及外部流程。
- 初始化反射调用器，支持 Kotlin suspend、默认参数及 Java `@Named` 参数。
- 接通命令匹配、频道检查、上下文参数和返回消息发送。
- 保留旧注解，并由旧包 Loader 入口复用新实现。
- 模块扫描同时包含新旧包；严格发送模式读取 `yuq.chat.strict`。

路由统一使用动态消费节点，直接在 MessageItem 链上保存下标和文本偏移。
消息进入匹配前不会按空格拆分，也不会把 At、Image 等消息项转成普通文本。

`@Path` 和 Action 声明中的空白用于划分一次消费的聚合组；消息在组间可以有空白，也可以没有。
每组可包含文字和多个 `{name}` 参数。例如 `给 {member} 禁言 {seconds} 秒` 分成五组，
每两个消费组之间的消息空白都可以直接忽略，也可以完全没有空白。
成员参数可以从 At 消息项或账号 ID 文本转换成当前群成员，因此同一声明可以匹配：

- `给@member禁言30秒`
- `给@member禁言 30 秒`
- `给StringId禁言30秒`

这里的 `@member` 表示 At 消息项，`StringId` 表示成员账号 ID 文本。
`@Path` 保留原声明，并作为前面的消费组；斜杠是普通消息文字。用 `\{`、`\}` 声明文字大括号。

文字跨相邻 Text 项消费，参数根据 Controller 方法的类型转换。
参数和后续文字都匹配成功才进入处理流程；失败恢复游标、捕获值和调用状态，并尝试其他消费范围或路由。
字符串可以包含空格，MessageBody 聚合保留消息项；At / Image 等参数保留原对象，At 或账号 ID 可以查找当前群成员。
数组和集合可以聚合多项，Map 支持字符串键与数字、布尔或枚举值，比如 `体质40精神60`、`张三男李四女`。
同一 Action 返回 SkipMe 后，每次分发只调用一次，避免重新划分参数时重复执行业务流程。

`BotParameterConverter.register` 可以注册领域类型转换，`RouterMatcher` 可以通过 `context.cursor` 实现自定义消费；
需要重试消费范围的匹配器实现 `match(context, next)`。默认匹配协议负责失败回退。
可跳过组、可选参数、固定格式重复与按类型消费处理器已经接入同一匹配协议，声明语法和示例见 [消息消费声明与参数处理器](command-pattern.md)。
机器人全局联系人查询、旧消息扩展类型和其他已有 TODO 的迁移仍是后续工作。

`AccountList` 的平台 ID 查询使用 `getByPlatformId`、`containsPlatformId`，避免与 Map 的 JVM 桥接方法冲突；
字符串账号 ID 仍使用 `map[id]`、`containsKey`。

验证命令：

```powershell
.\gradlew.bat :core:test :yuq-qq:build --console=plain
```

测试覆盖真实 Rain Loader、流程过滤、路由回退、频道限制、Java/Kotlin 调用、消息返回及旧包基础兼容。
消息消费组合测试包含以下声明：

- `给 {member} 禁言 {seconds} 秒`
- `把 {from} 的 {amount} 点 转给 {to}`
- `提醒 {member} 内容 {text} 完成`
- `配对 {left} {right} 确认`
- `@Path("管理 {member}")` 与 `设置 昵称 {nickname} 确认`
- `转发 {member} 内容 {payload} 完成`

每个成员位置分别使用 At 或账号 ID Text；相邻 Text 分别合并、按消费组保留或按 Unicode 码点碎片化。
组间空格数量穷举 0、1、4 的组合，补充全部边界 64 个空格、固定种子的 0–64 随机空格，
并单独检查每个边界 4096 个空格。共验证 13,258 组合法输入及 10,654 组非法输入。
合法输入检查完整消费、参数值和群成员对象、单次调用、消息回复及原消息链保持完整；
自由文本的内部空格、重复后续文字及转发内容中的原 At 对象也有单独断言。
非法输入覆盖未知成员、非数字、整数溢出、错误单位、缺失参数、数字中夹入 At、
消费组文字内部插入空格及末尾多余内容；匹配失败后恢复游标和参数，业务流程不执行。

QQ 模块目前只有平台接口，以上验证不包含真实 QQ Runtime 登录和收发。
