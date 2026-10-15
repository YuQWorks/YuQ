package yuq.controller

import com.icecreamqaq.yuq.annotation.BotAction as LegacyBotAction
import com.icecreamqaq.yuq.annotation.FriendAction as LegacyFriendAction
import com.icecreamqaq.yuq.annotation.GroupAction as LegacyGroupAction
import com.icecreamqaq.yuq.annotation.GroupController as LegacyGroupController
import com.icecreamqaq.yuq.annotation.GuildController as LegacyGuildController
import com.icecreamqaq.yuq.annotation.PrivateAction as LegacyPrivateAction
import com.icecreamqaq.yuq.annotation.PrivateController as LegacyPrivateController
import com.icecreamqaq.yuq.annotation.TemporaryAction as LegacyTemporaryAction
import rain.api.di.DiContext
import rain.controller.ActionCreator
import rain.controller.ControllerInstanceGetter
import rain.controller.ControllerLoader
import rain.controller.ProcessInvoker
import rain.controller.annotation.Path
import yuq.annotation.*
import yuq.controller.router.BotRootRouter
import yuq.controller.router.BotRouter
import yuq.controller.router.RouterMatcher
import yuq.internal.BotService
import java.lang.reflect.Method

open class BotControllerLoader(
    context: DiContext,
    val parameterConverter: BotParameterConverter = context.getBean(BotParameterConverter::class.java) ?: BotParameterConverter()
) : ControllerLoader<BotActionContext, BotRouter, BotRootInfo, BotActionInvoker, BotControllerProcessFlowInfo>(context) {

    private val rootInfo = BotRootInfo(BotRouter { true })
    var rootRouter: BotRootRouter? = null
        private set

    override fun findRootRouter(name: String): BotRootInfo = rootInfo

    /** Compile declaration groups without splitting or flattening the incoming message. */
    open fun readPattern(pattern: String, method: BotMethodInvoker? = null): List<RouterMatcher> =
        BotCommandPattern(parameterConverter, method).compile(pattern)

    @Deprecated("Use readPattern; declarations describe message consumption groups.")
    fun readPath(path: String): List<RouterMatcher> = readPattern(path)

    private fun patternToRouter(router: BotRouter, matchers: List<RouterMatcher>): BotRouter {
        var current = router
        matchers.forEach { matcher ->
            current = BotRouter(matcher).also { current.subRouters.add(it) }
        }
        return current
    }

    override fun controllerInfo(
        root: BotRootInfo,
        annotation: Annotation?,
        controllerClass: Class<*>,
        controllerInstance: Any
    ): BotControllerProcessFlowInfo {
        val channels = when (annotation) {
            is GroupController, is LegacyGroupController -> listOf(MessageChannel.Group.channel)
            is PrivateController, is LegacyPrivateController ->
                listOf(MessageChannel.Friend.channel, MessageChannel.GroupTemporary.channel)
            is GuildController, is LegacyGuildController -> listOf(MessageChannel.Guild.channel)
            else -> MessageChannel.entries.map { it.channel }
        }
        // Prefix bindings are compiled per action, after its generic parameter types are known.
        val router = BotRouter { true }.also { root.router.subRouters.add(it) }
        val instance = if (controllerInstance is ControllerInstanceGetter) controllerInstance() else controllerInstance
        return BotControllerProcessFlowInfo(
            controllerClass, instance, channels, router,
            controllerPattern = controllerClass.getAnnotation(Path::class.java)?.value ?: ""
        )
    }

    override fun makeProcessInvoker(
        controllerInfo: BotControllerProcessFlowInfo,
        processClass: Class<*>,
        processMethod: Method,
        processInstance: ControllerInstanceGetter
    ): ProcessInvoker<BotActionContext> = BotMethodInvoker(processMethod, processInstance, parameterConverter).init()

    override fun makeAction(
        rootRouter: BotRootInfo,
        controllerFlow: BotControllerProcessFlowInfo,
        controllerClass: Class<*>,
        actionMethod: Method,
        instanceGetter: ControllerInstanceGetter
    ): ActionCreator<BotActionContext, BotActionInvoker>? {
        val declaration = actionMethod.annotations.firstNotNullOfOrNull { annotation ->
            when (annotation) {
                is BotAction -> annotation.value to annotation.channel.toList()
                is LegacyBotAction -> annotation.value to annotation.channel.toList()
                is GroupAction -> annotation.value to listOf(MessageChannel.Group)
                is LegacyGroupAction -> annotation.value to listOf(MessageChannel.Group)
                is FriendAction -> annotation.value to listOf(MessageChannel.Friend)
                is LegacyFriendAction -> annotation.value to listOf(MessageChannel.Friend)
                is TemporaryAction -> annotation.value to listOf(MessageChannel.GroupTemporary)
                is LegacyTemporaryAction -> annotation.value to listOf(MessageChannel.GroupTemporary)
                is PrivateAction -> annotation.value to listOf(MessageChannel.Friend, MessageChannel.GroupTemporary)
                is LegacyPrivateAction -> annotation.value to listOf(MessageChannel.Friend, MessageChannel.GroupTemporary)
                else -> null
            }
        } ?: return null
        val (pattern, declaredChannels) = declaration
        val channels = declaredChannels.filter { it.channel in controllerFlow.controllerChannels }.toTypedArray()
        if (channels.isEmpty()) return null
        val action = BotMethodInvoker(actionMethod, instanceGetter, parameterConverter).apply { init() }
        val prefix = controllerFlow.controllerPattern
        val combined = if (prefix.isEmpty()) pattern else "$prefix $pattern"
        val matchers = readPattern(combined, action)
        val router = patternToRouter(controllerFlow.controllerRouter, matchers)
        return ActionCreator(controllerClass, actionMethod) { befores, afters, catches ->
            BotActionInvoker(controllerClass, actionMethod, channels, matchers, action, befores, afters, catches)
                .also { router.actions.add(it) }
        }
    }

    override fun buildRootRouter(router: BotRouter, actions: List<BotActionInvoker>): BotRootRouter =
        BotRootRouter(router, actions)

    override fun postLoad() {
        fun clearActions(router: BotRouter) {
            router.actions.clear()
            router.subRouters.forEach(::clearActions)
        }
        clearActions(rootInfo.router)
        rootRouter = buildRootInfo(rootInfo) as BotRootRouter
        context.getBean(BotService::class.java)?.rootRouter = rootRouter
    }
}
