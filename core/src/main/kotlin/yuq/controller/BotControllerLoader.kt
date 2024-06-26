package yuq.controller

import com.icecreamqaq.yuq.annotation.*
import com.icecreamqaq.yuq.controller.BotRootInfo
import yuq.controller.router.BotRouter
import yuq.controller.router.RouterMatcher
import rain.api.di.DiContext
import rain.controller.*
import rain.controller.annotation.After
import rain.controller.annotation.Before
import rain.controller.annotation.Catch
import rain.controller.annotation.Path
import rain.controller.simple.SimpleCatchMethodInvoker
import rain.function.annotation
import java.lang.reflect.Method
import kotlin.reflect.KProperty1

open class BotControllerLoader(
    context: DiContext
) : ControllerLoader<BotActionContext, BotRouter, BotRootInfo>(context) {

    private val rootInfo = BotRootInfo(BotRouter { true })

    override fun findRootRouter(name: String) = rootInfo

    fun readPath(path: String): List<RouterMatcher> {
        TODO()
    }

    override fun controllerInfo(
        root: BotRootInfo,
        annotation: Annotation?,
        controllerClass: Class<*>,
        instanceGetter: ControllerInstanceGetter
    ): ControllerProcessFlowInfo<BotActionContext, BotRouter>? {
        val channels = controllerChannel(annotation, controllerClass) ?: return null
        var controllerRouter = root.router
        controllerClass.annotation<Path> {
            readPath(value).forEach { m ->
                controllerRouter = controllerRouter.subRouters.firstOrNull { it.matcher == m }
                    ?: BotRouter(m).also { controllerRouter.subRouters.add(it) }
            }
        }

        return ControllerProcessFlowInfo(controllerClass, channels, controllerRouter)
    }

    private fun controllerChannel(annotation: Annotation?, controllerClass: Class<*>): List<String>? {
        if (annotation == null) return null
        return when (annotation) {
            is GroupController -> arrayListOf(MessageChannel.Group.channel)
            is PrivateController -> arrayListOf(MessageChannel.Friend.channel, MessageChannel.GroupTemporary.channel)
            else -> arrayListOf()
        }
    }

    override fun makeAfter(
        afterAnnotation: After,
        controllerClass: Class<*>,
        afterMethod: Method,
        instanceGetter: ControllerInstanceGetter
    ): ProcessInfo<BotActionContext>? =
        ProcessInfo(
            afterAnnotation.weight,
            afterAnnotation.except,
            afterAnnotation.only,
            BotMethodInvoker(afterMethod, instanceGetter)
        )

    override fun makeBefore(
        beforeAnnotation: Before,
        controllerClass: Class<*>,
        beforeMethod: Method,
        instanceGetter: ControllerInstanceGetter
    ): ProcessInfo<BotActionContext>? =
        ProcessInfo(
            beforeAnnotation.weight,
            beforeAnnotation.except,
            beforeAnnotation.only,
            BotMethodInvoker(beforeMethod, instanceGetter)
        )

    override fun makeCatch(
        catchAnnotation: Catch,
        controllerClass: Class<*>,
        catchMethod: Method,
        instanceGetter: ControllerInstanceGetter
    ): ProcessInfo<BotActionContext>? =
        ProcessInfo(
            catchAnnotation.weight,
            catchAnnotation.except,
            catchAnnotation.only,
            SimpleCatchMethodInvoker(catchAnnotation.error.java, BotMethodInvoker(catchMethod, instanceGetter))
        )

    override fun postLoad() {
        val actionList = ArrayList<ActionInfo<BotActionContext>>()
        rootInfo.controllers.forEach {
            it.actions.forEach {
                actionList.add(
                    ActionInfo(
                        it.actionClass,
                        it.actionMethod,
                        it.creator()
                    )
                )
            }
        }

//        botService.rootRouter = BotRootRouter(rootInfo.router, actionList)
    }

    override fun makeAction(
        rootRouter: BotRootInfo,
        controllerFlow: ControllerProcessFlowInfo<BotActionContext, BotRouter>,
        controllerClass: Class<*>,
        actionMethod: Method,
        instanceGetter: ControllerInstanceGetter
    ): ActionProcessFlowInfo<BotActionContext>? {
        var path = ""
        var channels: Array<MessageChannel> = emptyArray()
        actionMethod.annotation<GroupAction> {
            path = value
            channels = arrayOf(MessageChannel.Group)
        }
        actionMethod.annotation<PrivateAction> {
            path = value
            channels = arrayOf(MessageChannel.GroupTemporary, MessageChannel.Friend)
        }
        actionMethod.annotation<FriendAction> {
            path = value
            channels = arrayOf(MessageChannel.Friend)
        }
        actionMethod.annotation<TemporaryAction> {
            path = value
            channels = arrayOf(MessageChannel.GroupTemporary)
        }

        if (channels.isEmpty()) return null

        val actionName = actionMethod.name

        val actionFlow = ActionProcessFlowInfo<BotActionContext>(controllerClass, actionMethod)
        val matchers = readPath(path)
        fun checkPf(property: KProperty1<ProcessFlowInfo<BotActionContext>, MutableList<ProcessInfo<BotActionContext>>>): Array<ProcessInvoker<BotActionContext>> =
            ArrayList<ProcessInfo<BotActionContext>>()
                .apply {
                    val checkPi = { it: ProcessInfo<BotActionContext> ->
                        if (actionName !in it.except && it.only.isNotEmpty() && actionName in it.only) add(it)
                    }
                    property.get(rootRouter).forEach(checkPi)
                    property.get(controllerFlow).forEach(checkPi)
                    property.get(actionFlow).forEach(checkPi)
                    sortBy { it.priority }
                }
                .map { it.invoker }
                .toTypedArray()

//        actionFlow.creator = ActionInvokerCreator {
//            BotActionInvoker(
//                channels,
//                matchers,
//                BotMethodInvoker(actionMethod, instanceGetter),
//                checkPf(ProcessFlowInfo<BotActionContext>::beforeProcesses),
//                checkPf(ProcessFlowInfo<BotActionContext>::afterProcesses),
//                checkPf(ProcessFlowInfo<BotActionContext>::catchProcesses)
//            )
//        }
        return actionFlow
    }

}