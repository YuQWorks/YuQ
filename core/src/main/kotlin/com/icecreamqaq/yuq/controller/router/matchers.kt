package com.icecreamqaq.yuq.controller.router

import yuq.controller.BotActionContext
import yuq.controller.router.RouterMatcher

class StaticMatcher(val str: String) : RouterMatcher {
    override fun invoke(context: BotActionContext): Boolean {
        return context.matcherItem.isNext(str)
    }
}