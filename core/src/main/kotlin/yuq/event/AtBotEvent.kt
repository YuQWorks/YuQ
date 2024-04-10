package yuq.event

import com.icecreamqaq.yuq.contact.GuildMember
import yuq.Bot
import yuq.contact.Contact
import yuq.contact.Friend
import yuq.contact.Group
import yuq.contact.GroupMember


open class AtBotEvent(open val type: Int, open val sender: Contact, open val source: Contact) : BotEvent {
    open class ByGroup(type: Int, override val sender: GroupMember, override val source: Group) :
        AtBotEvent(type, sender, source)

//    open class ByGuild(type: Int, override val sender: GuildMember, override val source: Channel, val guild: Guild) :
//        AtBotEvent(type, sender, source)

    open class ByPrivate(type: Int, sender: Contact, source: Contact) : AtBotEvent(type, sender, source) {
        open class ByFriend(type: Int, override val sender: Friend, override val source: Friend) :
            ByPrivate(type, sender, source)

        open class ByTemp(type: Int, override val sender: GroupMember, override val source: GroupMember) :
            ByPrivate(type, sender, source)
    }

    override val bot: Bot
        get() = sender.bot
}