package yuq.controller.router

import yuq.controller.BotActionContext

data class StaticMatcher(val str: String) : RouterMatcher {
    override fun invoke(context: BotActionContext): Boolean = with(context.matcherItem) {
        skipWhitespace()
        isNext(str) && isBoundary()
    }
}

data class ParameterMatcher(val name: String) : RouterMatcher {
    override fun invoke(context: BotActionContext): Boolean {
        val value = context.matcherItem.readArgument() ?: return false
        context[name] = value
        return true
    }
}
