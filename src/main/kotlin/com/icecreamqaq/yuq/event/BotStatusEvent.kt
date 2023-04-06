package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.Bot

open class BotStatusEvent(override val bot: Bot) : Event(), BotEvent{
    open class Online(bot: Bot) : BotStatusEvent(bot)
    open class Offline(bot: Bot) : BotStatusEvent(bot)
    open class ReOnline(bot: Bot) : BotStatusEvent(bot)
}