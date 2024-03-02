package com.icecreamqaq.yuq.controller.router

import com.icecreamqaq.yuq.controller.BotActionContext
import rain.controller.ActionInfo
import rain.controller.RootRouter

class BotRootRouter(
    router: BotRouter,
    actions: List<ActionInfo<BotActionContext>>
) : RootRouter<BotActionContext, BotRouter>(router, actions)