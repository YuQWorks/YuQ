package com.icecreamqaq.yuq.controller.router

import com.icecreamqaq.yuq.controller.BotActionContext

class StaticMatcher(val str: String) : RouterMatcher {
    override fun invoke(context: BotActionContext): Boolean {
        return context.matcherItem.isNext(str)
    }
}