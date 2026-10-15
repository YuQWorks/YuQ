package yuq.controller.router

import rain.controller.Router
import yuq.controller.BotActionContext
import yuq.controller.BotActionInvoker

class BotRouter(val matcher: RouterMatcher) : Router {
    val subRouters = ArrayList<BotRouter>()
    val actions = ArrayList<BotActionInvoker>()

    suspend operator fun invoke(context: BotActionContext): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            for (router in subRouters) {
                context.rollback(checkpoint)
                if (router.matcher.match(context) { router(context) }) {
                    matched = true
                    return true
                }
            }
            for (action in actions) {
                context.rollback(checkpoint)
                if (context.cursor.hasRemaining() || context.channel !in action.channels) continue
                // A completed match that returns SkipMe must not repeat business processes
                // merely because another parameter partition also matches this action.
                if (!context.attemptedActions.add(action)) continue
                context.result = null
                context.runtimeError = null
                context.actionInvoker = action
                if (action(context)) {
                    matched = true
                    return true
                }
            }
            return false
        } finally {
            if (!matched) context.rollback(checkpoint)
        }
    }
}
