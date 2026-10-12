package yuq.controller.router

import yuq.controller.BotActionContext
import rain.controller.RootRouter
import yuq.controller.BotActionInvoker

class BotRootRouter(
    router: BotRouter,
    actions: List<BotActionInvoker>
) : RootRouter<BotActionContext, BotRouter, BotActionInvoker>(router, actions) {
    suspend operator fun invoke(context: BotActionContext): Boolean = router(context)
}
