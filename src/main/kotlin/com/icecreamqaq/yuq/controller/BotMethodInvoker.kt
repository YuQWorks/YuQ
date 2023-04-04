package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ControllerInstanceGetter
import com.IceCreamQAQ.Yu.controller.simple.SimpleKJReflectMethodInvoker
import java.lang.reflect.Method

open class BotMethodInvoker(
    method: Method,
    instance: ControllerInstanceGetter
) : SimpleKJReflectMethodInvoker<BotActionContext,(BotActionContext) -> Any?>(method, instance) {

    override fun getParam(param: MethodParam<(BotActionContext) -> Any?>, context: BotActionContext): Any? {

        TODO("Not yet implemented")
    }

    override fun initParam(method: Method, params: Array<MethodParam<(BotActionContext) -> Any?>>) {
        TODO("Not yet implemented")
    }

}