package com.icecreamqaq.yuq.controller.router

import yuq.controller.BotActionContext
import rain.controller.ActionInfo
import rain.controller.RootRouter
import yuq.controller.router.BotRouter

class BotRootRouter(
    router: BotRouter,
    actions: List<ActionInfo<BotActionContext>>
) : RootRouter<BotActionContext, BotRouter>(router, actions)