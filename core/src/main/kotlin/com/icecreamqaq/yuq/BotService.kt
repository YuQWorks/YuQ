package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.annotation.Internal
import com.icecreamqaq.yuq.contact.ContactSession
import org.slf4j.LoggerFactory
import rain.api.event.EventBus
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

@Internal
class BotService(
    private val eventBus: EventBus,
) {
    companion object {
        private val log = LoggerFactory.getLogger(BotService::class.java)
        private val sessions = WeakHashMap<Bot, ConcurrentHashMap<String, ContactSession>>()

        @Synchronized
        fun getContextSession(bot: Bot, id: String): ContactSession =
            sessions.getOrPut(bot) { ConcurrentHashMap() }.computeIfAbsent(id) { ContactSession(it) }
    }


}
