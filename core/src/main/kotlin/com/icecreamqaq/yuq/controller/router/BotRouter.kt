package com.icecreamqaq.yuq.controller.router

import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.BotActionInvoker
import com.icecreamqaq.yuq.controller.MatcherItem
import rain.controller.Router

class BotRouter(val matcher: RouterMatcher) : Router {

    val subRouters = ArrayList<BotRouter>()
    val actions = ArrayList<BotActionInvoker>()

    suspend operator fun invoke(context: BotActionContext): Boolean {
        val mi = context.matcherItem
        val mark = mi.mark()

        if (subRouters.any(mi, mark) { it.matcher(context) && it(context) }) return true
//        return actions.any(mi, mark) { it(context) }
        return false
    }

    inline fun <T> Iterable<T>.any(mi: MatcherItem, mark: Pair<Int, Int>, predicate: (T) -> Boolean): Boolean {
        if (this is Collection && isEmpty()) return false
        for (element in this) {
            mi.reMark(mark)
            if (predicate(element)) return true
        }
        return false
    }

}