package yuq.controller.router

import yuq.controller.BotActionContext

class StaticMatcher(val str: String) : RouterMatcher {
    override fun invoke(context: BotActionContext): Boolean {
        return context.matcherItem.isNext(str)
    }
}