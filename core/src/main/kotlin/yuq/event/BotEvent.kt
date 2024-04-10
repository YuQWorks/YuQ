package yuq.event

import rain.api.event.Event
import yuq.Bot

interface BotEvent : Event {
    val bot: Bot
}