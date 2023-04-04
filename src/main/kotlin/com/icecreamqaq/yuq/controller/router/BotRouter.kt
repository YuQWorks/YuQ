package com.icecreamqaq.yuq.controller.router

import com.IceCreamQAQ.Yu.controller.Router
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.BotActionInvoker

class BotRouter(val matcher: RouterMatcher) : Router {

    val subRouters = ArrayList<BotRouter>()
    val actions = ArrayList<BotActionInvoker>()

    suspend operator fun invoke(context: BotActionContext): Boolean {
        if (subRouters.any { it.matcher(context) && it(context) }) return true
        return actions.any { it.invoke(context) }
    }

}