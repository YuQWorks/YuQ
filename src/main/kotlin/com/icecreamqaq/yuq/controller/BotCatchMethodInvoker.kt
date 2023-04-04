package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.ControllerInstanceGetter
import java.lang.reflect.Method

class BotCatchMethodInvoker(
    method: Method,
    instance: ControllerInstanceGetter,
    val errorType: Class<out Throwable>
) : BotMethodInvoker(method, instance) {

    override suspend fun invoke(context: BotActionContext): Any? {
        if (context.runtimeError != null && errorType.isAssignableFrom(context.runtimeError!!::class.java)) {
            return super.invoke(context)
        }
        return null
    }

}