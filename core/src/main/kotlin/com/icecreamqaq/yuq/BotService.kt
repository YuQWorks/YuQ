package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.annotation.Internal
import org.slf4j.LoggerFactory
import rain.api.event.EventBus

@Internal
class BotService(
    private val eventBus: EventBus,
) {
    companion object {
        private val log = LoggerFactory.getLogger(BotService::class.java)
    }


}