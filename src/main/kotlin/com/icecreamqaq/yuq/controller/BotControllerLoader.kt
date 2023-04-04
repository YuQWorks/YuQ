package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.annotation
import com.IceCreamQAQ.Yu.annotation.After
import com.IceCreamQAQ.Yu.annotation.Before
import com.IceCreamQAQ.Yu.annotation.Catch
import com.IceCreamQAQ.Yu.annotation.Path
import com.IceCreamQAQ.Yu.controller.*
import com.IceCreamQAQ.Yu.controller.dss.router.NamedVariableMatcher
import com.IceCreamQAQ.Yu.controller.dss.router.RegexMatcher
import com.IceCreamQAQ.Yu.di.YuContext
import com.icecreamqaq.yuq.annotation.GroupAction
import com.icecreamqaq.yuq.annotation.GroupController
import com.icecreamqaq.yuq.annotation.PrivateAction
import com.icecreamqaq.yuq.annotation.PrivateController
import com.icecreamqaq.yuq.controller.router.BotRouter
import com.icecreamqaq.yuq.controller.router.RouterMatcher
import java.lang.reflect.Method

open class BotControllerLoader(
    context: YuContext
) : ControllerLoader<BotActionContext, BotRouter, BotRootInfo>(context) {

    private val rootInfo = BotRootInfo(BotRouter { true })

    override fun findRootRouter(name: String) = rootInfo

    override fun controllerInfo(
        root: BotRootInfo,
        annotation: Annotation?,
        controllerClass: Class<*>,
        instanceGetter: ControllerInstanceGetter
    ): ControllerProcessFlowInfo<BotActionContext, BotRouter>? {
        val channels = controllerChannel(annotation, controllerClass) ?: return null
        var controllerRouter = root.router
//        controllerClass.annotation<Path>()?.value?.split("/")?.forEach {
//            controllerRouter = makePathMatcher(it).let { (path, matchers) ->
//                if (matchers.isEmpty()) getSubStaticRouter(controllerRouter, path)
//                else {
//                    getSubDynamicRouter(
//                        controllerRouter,
//                        if (path.isEmpty() && matchers.size == 1 && matchers[0].second == ".*")
//                            NamedVariableMatcher(matchers[0].first)
//                        else RegexMatcher(path, matchers.map { item -> item.first }.toTypedArray())
//                    )
//                }
//            }
//        }

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
            BotCatchMethodInvoker(catchMethod, instanceGetter, catchAnnotation.error.java)
        )

    override fun postLoad() {
        TODO("Not yet implemented")
    }

    override fun makeAction(
        rootRouter: BotRootInfo,
        controllerFlow: ControllerProcessFlowInfo<BotActionContext, BotRouter>,
        controllerClass: Class<*>,
        actionMethod: Method,
        instanceGetter: ControllerInstanceGetter
    ): ActionProcessFlowInfo<BotActionContext>? {
        TODO("Not yet implemented")
    }

}