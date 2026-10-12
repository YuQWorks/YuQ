package com.icecreamqaq.yuq.controller.router

import yuq.controller.BotActionContext
import rain.controller.RootRouter
import yuq.controller.BotActionInvoker
import yuq.controller.router.BotRouter

class BotRootRouter(
    router: BotRouter,
    actions: List<BotActionInvoker>
) : RootRouter<BotActionContext, BotRouter, BotActionInvoker>(router, actions)
