package com.icecreamqaq.yuq.event

import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.contact.*
import com.icecreamqaq.yuq.message.Message
import rain.event.events.AbstractCancelAbleEvent


open class MessageEvent(open val sender: Contact, val message: Message) : AbstractCancelAbleEvent(), BotEvent {
    override val bot: Bot
        get() = sender.bot
}

open class GroupMessageEvent(override val sender: GroupMember, val group: Group, message: Message) :
    MessageEvent(sender, message)

open class GuildMessageEvent(
    override val sender: GuildMember,
    val guild: Guild,
    val channel: Channel,
    message: Message
) : MessageEvent(sender, message)

open class PrivateMessageEvent(sender: Contact, message: Message) : MessageEvent(sender, message) {
    open class FriendMessage(override val sender: Friend, message: Message) : PrivateMessageEvent(sender, message)
    open class TempMessage(override val sender: GroupMember, message: Message) : PrivateMessageEvent(sender, message)
}