package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.annotation.Internal
import com.icecreamqaq.yuq.contact.*
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.MessageChannel
import com.icecreamqaq.yuq.controller.router.BotRootRouter
import com.icecreamqaq.yuq.error.SendMessageFailedByCancel
import com.icecreamqaq.yuq.error.SendMessageFailedByTimeout
import com.icecreamqaq.yuq.event.*
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.util.liteMessage
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import rain.api.event.EventBus
import rain.di.Config

@Internal
class BotService(
    private val eventBus: EventBus,
) {
    companion object {
        private val log = LoggerFactory.getLogger(BotService::class.java)
    }


}