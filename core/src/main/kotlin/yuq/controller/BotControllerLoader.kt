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
import yuq.controller.router.ParameterMatcher
import yuq.controller.router.RouterMatcher
import yuq.controller.router.StaticMatcher
import yuq.internal.BotService
import java.lang.reflect.Method

open class BotControllerLoader(
    context: DiContext
) : ControllerLoader<BotActionContext, BotRouter, BotRootInfo, BotActionInvoker, BotControllerProcessFlowInfo>(context) {

    private val rootInfo = BotRootInfo(BotRouter { true })

    var rootRouter: BotRootRouter? = null
        private set

    override fun findRootRouter(name: String): BotRootInfo = rootInfo

    fun readPath(path: String): List<RouterMatcher> =
        path.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { token ->
            if (token.startsWith('{') && token.endsWith('}')) {
                val name = token.substring(1, token.lastIndex)
                require(name.isNotBlank() && '{' !in name && '}' !in name) { "Invalid route parameter: $token" }
                ParameterMatcher(name)
            } else {
                require('{' !in token && '}' !in token) { "Route parameters must occupy a whole token: $token" }
                StaticMatcher(token)
            }
        }

    private fun pathToRouter(router: BotRouter, matchers: List<RouterMatcher>): BotRouter {
        var current = router
        matchers.forEach { matcher ->
            current = current.subRouters.firstOrNull { it.matcher == matcher }
                ?: BotRouter(matcher).also { current.subRouters.add(it) }
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
        val router = pathToRouter(root.router, readPath(controllerClass.getAnnotation(Path::class.java)?.value ?: ""))
        // DEV14's loader supplies a getter here, although the public signature accepts Any.
        val instance = if (controllerInstance is ControllerInstanceGetter) controllerInstance() else controllerInstance
        return BotControllerProcessFlowInfo(controllerClass, instance, channels, router)
    }

    override fun makeProcessInvoker(
        controllerInfo: BotControllerProcessFlowInfo,
        processClass: Class<*>,
        processMethod: Method,
        processInstance: ControllerInstanceGetter
    ): ProcessInvoker<BotActionContext> = BotMethodInvoker(processMethod, processInstance).init()

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
        val (path, declaredChannels) = declaration
        val channels = declaredChannels.filter { it.channel in controllerFlow.controllerChannels }.toTypedArray()
        if (channels.isEmpty()) return null
        val matchers = readPath(path.removePrefix("/"))
        val parent = if (path.startsWith('/')) rootRouter.router else controllerFlow.controllerRouter
        val router = pathToRouter(parent, matchers)
        val action = BotMethodInvoker(actionMethod, instanceGetter).init()
        return ActionCreator(controllerClass, actionMethod) { befores, afters, catches ->
            BotActionInvoker(controllerClass, actionMethod, channels, matchers, action, befores, afters, catches)
                .also { router.actions.add(it) }
        }
    }

    override fun buildRootRouter(router: BotRouter, actions: List<BotActionInvoker>): BotRootRouter =
        BotRootRouter(router, actions)

    override fun postLoad() {
        // Loading can run once per triggering annotation; replace previously built invokers.
        fun clearActions(router: BotRouter) {
            router.actions.clear()
            router.subRouters.forEach(::clearActions)
        }
        clearActions(rootInfo.router)
        rootRouter = buildRootInfo(rootInfo) as BotRootRouter
        context.getBean(BotService::class.java)?.rootRouter = rootRouter
    }
}
