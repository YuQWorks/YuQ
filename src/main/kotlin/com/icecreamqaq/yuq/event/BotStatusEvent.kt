package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.Bot

open class BotStatusEvent(override val bot: Bot) : Event(), BotEvent

open class BotOnlineEvent(bot: Bot) : BotStatusEvent(bot)
open class BotOfflineEvent(bot: Bot) : BotStatusEvent(bot)
open class BotReOnlineEvent(bot: Bot) : BotStatusEvent(bot)