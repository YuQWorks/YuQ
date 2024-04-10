package yuq.event

import rain.event.events.AbstractCancelAbleEvent
import yuq.Bot
import yuq.contact.Contact
import yuq.contact.Friend
import yuq.contact.Group
import yuq.contact.GroupMember
import yuq.message.Message

open class MessageEvent(open val sender: Contact, val message: Message) : AbstractCancelAbleEvent(), BotEvent {
    override val bot: Bot
        get() = sender.bot

    open class GroupMessage(override val sender: GroupMember, val group: Group, message: Message) :
        MessageEvent(sender, message)

//    open class GuildMessage(
//        override val sender: GuildMember,
//        val guild: Guild,
//        val channel: Channel,
//        message: Message
//    ) : MessageEvent(sender, message)

    open class PrivateMessage(sender: Contact, message: Message) : MessageEvent(sender, message) {
        open class FriendMessage(override val sender: Friend, message: Message) : PrivateMessage(sender, message)
        open class TempMessage(override val sender: GroupMember, message: Message) : PrivateMessage(sender, message)
    }
}