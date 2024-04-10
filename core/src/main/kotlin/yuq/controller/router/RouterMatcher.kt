package yuq.controller.router

import yuq.controller.BotActionContext

fun interface RouterMatcher {
    operator fun invoke(context: BotActionContext): Boolean
}