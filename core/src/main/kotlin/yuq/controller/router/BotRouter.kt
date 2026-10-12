package yuq.controller.router

import rain.controller.Router
import yuq.controller.BotActionContext
import yuq.controller.BotActionInvoker

class BotRouter(val matcher: RouterMatcher) : Router {

    val subRouters = ArrayList<BotRouter>()
    val actions = ArrayList<BotActionInvoker>()

    suspend operator fun invoke(context: BotActionContext): Boolean {
        val item = context.matcherItem
        val mark = item.mark()
        val values = context.saveValues()
        fun restore() {
            item.reMark(mark)
            context.restoreValues(values)
        }

        for (router in subRouters) {
            restore()
            if (router.matcher(context) && router(context)) return true
        }
        for (action in actions) {
            restore()
            if (item.hasRemaining()) continue
            context.result = null
            context.runtimeError = null
            context.actionInvoker = action
            if (action(context)) return true
        }
        restore()
        return false
    }
}
