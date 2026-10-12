# Rain DEV14 适配

以 `yuq.*` 为后续开发的主 API，功能逐步迁移；在迁移完成前继续保留 `com.icecreamqaq.yuq.*`。

本次完成 Controller 链路对 Rain `1.0.0-DEV14` 的适配：

- `BotRouter` 与 `BotActionInvoker` 分开，根路由和 Controller 流程分别声明路由、调用器类型。
- 使用 `ActionCreator` 创建调用器，交由 Rain 组装、排序和过滤 Before / After / Catch 及外部流程。
- 初始化反射调用器，支持 Kotlin suspend、默认参数及 Java `@Named` 参数。
- 接通命令匹配、频道检查、上下文参数和返回消息发送。
- 保留旧注解，并由旧包 Loader 入口复用新实现。
- 模块扫描同时包含新旧包；严格发送模式读取 `yuq.chat.strict`。

当前路由按空白分词，支持字面量和独立的 `{name}` 参数。例如 `add {left} {right}`。
类上的 `@Path` 提供前缀，Action 路径以 `/` 开头时从根路由匹配。
参数可注入上下文、机器人、消息、联系人、会话、异常，以及路由中的字符串、基础数值和枚举。
完整联系人查询、旧消息扩展类型和其他已有 TODO 的迁移仍是后续工作。

`AccountList` 的平台 ID 存在性查询改为 `containsPlatformId`，避免与 `Map.containsKey` 的 JVM 签名冲突；字符串账号 ID 仍使用 `containsKey`。

验证命令：

```powershell
.\gradlew.bat :core:test :yuq-qq:build --console=plain
```

测试覆盖真实 Rain Loader、流程过滤、路由回退、频道限制、Java/Kotlin 调用、消息返回及旧包基础兼容。
QQ 模块目前只有平台接口，以上验证不包含真实 QQ Runtime 登录和收发。
