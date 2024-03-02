package com.icecreamqaq.yuq.event

import com.icecreamqaq.yuq.Bot

open class BotStatusEvent(override val bot: Bot) : BotEvent{
    open class Online(bot: Bot) : BotStatusEvent(bot)
    open class Offline(bot: Bot) : BotStatusEvent(bot)
    open class ReOnline(bot: Bot) : BotStatusEvent(bot)
}