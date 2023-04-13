package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.CancelEvent
import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.contact.*
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource

interface BotEvent {
    val bot: Bot
}

open class MessageEvent(open val sender: Contact, val message: Message) : Event(), CancelEvent, BotEvent {
    override fun cancelAble() = true
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

open class MessageRecallEvent(
    open val sender: Contact, open val operator: Contact, val messageId: Int
) : Event(), BotEvent {
    override val bot: Bot
        get() = sender.bot
}

open class PrivateRecallEvent(sender: Contact, operator: Contact, messageId: Int) :
    MessageRecallEvent(sender, operator, messageId)

open class GroupRecallEvent(
    val group: Group,
    override val sender: GroupMember,
    override val operator: GroupMember,
    messageId: Int
) : MessageRecallEvent(sender, operator, messageId)

open class FriendListEvent(override val bot: Bot) : Event(), BotEvent
open class FriendAddEvent(val friend: Friend) : FriendListEvent(friend.bot)
open class FriendDeleteEvent(val friend: Friend) : FriendListEvent(friend.bot)

open class GroupListEvent(override val bot: Bot) : Event(), BotEvent
open class BotJoinGroupEvent(val group: Group) : GroupListEvent(group.bot)

/***
 * com.icecreamqaq.bot.Bot 从某个群离开。
 * 当事件响应前，group 就已经从列表中被移出。
 */
open class BotLeaveGroupEvent(val group: Group) : GroupListEvent(group.bot) {
    /***
     * com.icecreamqaq.bot.Bot 主动退出某群。
     */
    open class Leave(group: Group) : BotLeaveGroupEvent(group)

    /***
     * com.icecreamqaq.bot.Bot 因为某些特殊原因离开某群（其他客户端主动退出，群解散，群被强制解散等等）
     */
    open class Other(group: Group) : BotLeaveGroupEvent(group)

    /***
     * com.icecreamqaq.bot.Bot 被某群移出。
     */
    open class Kick(val operator: GroupMember) : BotLeaveGroupEvent(operator.group)
}

open class NewRequestEvent(override val bot: Bot, val message: String) : Event(), CancelEvent, BotEvent {
    override fun cancelAble() = true
    var accept: Boolean? = null
    var rejectMessage: String = ""
}

open class NewFriendRequestEvent(
    bot: Bot,
    val qq: UserInfo,
    val group: Group?,
    message: String
) : NewRequestEvent(bot, message)

open class GroupInviteEvent(bot: Bot, val group: GroupInfo, val qq: UserInfo, message: String) :
    NewRequestEvent(bot, message)

open class GroupMemberRequestEvent(
    val group: Group, val qq: UserInfo, message: String
) : NewRequestEvent(group.bot, message),
    CancelEvent {
    override fun cancelAble() = true
    val blackList = false
}

open class GroupMemberEvent(val group: Group, val member: GroupMember) : Event(), BotEvent {
    override val bot: Bot
        get() = group.bot
}

open class GroupMemberJoinEvent(group: Group, member: GroupMember) : GroupMemberEvent(group, member) {
    open class Join(group: Group, member: GroupMember) : GroupMemberJoinEvent(group, member)
    open class Invite(group: Group, member: GroupMember, val inviter: GroupMember) : GroupMemberJoinEvent(group, member)
}

@Deprecated("群事件结构调整，使得命名语义更加清晰。", ReplaceWith("GroupMemberJoinEvent.Invite"))
open class GroupMemberInviteEvent(group: Group, member: GroupMember, inviter: GroupMember) :
    GroupMemberJoinEvent.Invite(group, member, inviter)


open class GroupMemberLeaveEvent(group: Group, member: GroupMember) : GroupMemberEvent(group, member) {
    open class Leave(group: Group, member: GroupMember) : GroupMemberLeaveEvent(group, member)
    open class Kick(group: Group, member: GroupMember, val operator: GroupMember) : GroupMemberLeaveEvent(group, member)
}

@Deprecated("群事件结构调整，使得命名语义更加清晰。", ReplaceWith("GroupMemberLeaveEvent.Kick"))
open class GroupMemberKickEvent(group: Group, member: GroupMember, operator: GroupMember) :
    GroupMemberLeaveEvent.Kick(group, member, operator)

open class GroupBanMemberEvent(group: Group, member: GroupMember, val operator: GroupMember, val time: Int) :
    GroupMemberEvent(group, member)

open class GroupUnBanMemberEvent(group: Group, member: GroupMember, val operator: GroupMember) : GroupMemberEvent(group, member)
open class GroupBanBotEvent(group: Group, member: GroupMember, val operator: GroupMember, val time: Int) :
    GroupMemberEvent(group, member)

open class GroupUnBanBotEvent(group: Group, member: GroupMember, val operator: GroupMember) : GroupMemberEvent(group, member)

open class ContextSessionCreateEvent(override val bot: Bot, session: ContactSession) : Event(), BotEvent
open class ActionContextInvokeEvent(val context: BotActionContext) : Event(), CancelEvent {
    override fun cancelAble() = true
    open class Per(context: BotActionContext) : ActionContextInvokeEvent(context)

    open class Post(context: BotActionContext, val routerMatchFlag: Boolean) : ActionContextInvokeEvent(context)
}

open class SendMessageEvent(val sendTo: Contact, val message: Message) : Event(), BotEvent {
    open class Per(sendTo: Contact, message: Message) : SendMessageEvent(sendTo, message), CancelEvent {
        override fun cancelAble() = true
    }

    open class Post(sendTo: Contact, message: Message, val messageSource: MessageSource) :
        SendMessageEvent(sendTo, message)

    override val bot: Bot
        get() = sendTo.bot
}

// 消息发送未达预期事件。
open class SendMessageInvalidEvent(
    val sendTo: Contact,
    val message: Message
) : Event(), BotEvent {
    // 发送消息被取消事件。
    class ByCancel(sendTo: Contact, message: Message) : SendMessageInvalidEvent(sendTo, message)

    /*** 读取发送消息超时事件。
     * 消息发送成功了，但是消息被拒发。
     * 他有别于消息发送失败，因为消息发送这个过程完成了。
     * 一般指消息正常上报给了服务端，但服务端拒绝广播本消息。
     * 也就是俗称的吞消息。
     */
    class ByReadTimeout(sendTo: Contact, message: Message) : SendMessageInvalidEvent(sendTo, message)

    override val bot: Bot
        get() = sendTo.bot
}


open class ClickEvent(open val operator: Contact, val action: String, val suffix: String) : Event(), BotEvent {
    override val bot: Bot
        get() = operator.bot
}

open class ClickBotEvent(operator: Contact, action: String, suffix: String) : ClickEvent(operator, action, suffix) {
    open class Private(operator: Contact, action: String, suffix: String) : ClickBotEvent(operator, action, suffix) {
        open class FriendClick(override val operator: Friend, action: String, suffix: String) :
            Private(operator, action, suffix)

        open class TempClick(override val operator: GroupMember, action: String, suffix: String) :
            Private(operator, action, suffix)
    }

    open class Group(override val operator: GroupMember, action: String, suffix: String) :
        ClickBotEvent(operator, action, suffix)
}

open class AtBotEvent(open val type: Int, open val sender: Contact, open val source: Contact) : Event(), BotEvent {
    open class ByGroup(type: Int, override val sender: GroupMember, override val source: Group) :
        AtBotEvent(type, sender, source)

    open class ByGuild(type: Int, override val sender: GuildMember, override val source: Channel, val guild: Guild) :
        AtBotEvent(type, sender, source)

    open class ByPrivate(type: Int, sender: Contact, source: Contact) : AtBotEvent(type, sender, source) {
        open class ByFriend(type: Int, override val sender: Friend, override val source: Friend) :
            ByPrivate(type, sender, source)

        open class ByTemp(type: Int, override val sender: GroupMember, override val source: GroupMember) :
            ByPrivate(type, sender, source)
    }

    override val bot: Bot
        get() = sender.bot
}

open class ClickSomeBodyEvent(operator: Contact, open val target: Contact, action: String, suffix: String) :
    ClickEvent(operator, action, suffix) {
    open class Private(operator: Contact, target: Contact, action: String, suffix: String) :
        ClickSomeBodyEvent(operator, target, action, suffix)

    open class Group(override val operator: GroupMember, override val target: GroupMember, action: String, suffix: String) :
        ClickSomeBodyEvent(operator, target, action, suffix)
}
