package com.icecreamqaq.yuq.controller.router

import com.icecreamqaq.yuq.controller.BotActionContext

fun interface RouterMatcher {
    operator fun invoke(context: BotActionContext): Boolean
}