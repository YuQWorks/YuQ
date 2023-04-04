package com.icecreamqaq.yuq.controller.router

import com.IceCreamQAQ.Yu.controller.ActionInfo
import com.IceCreamQAQ.Yu.controller.RootRouter
import com.icecreamqaq.yuq.controller.BotActionContext

class BotRootRouter(
    router: BotRouter,
    actions: List<ActionInfo<BotActionContext>>
) : RootRouter<BotActionContext, BotRouter>(router, actions)