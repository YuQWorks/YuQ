package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.CancelEvent
import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource

open class MessageEvent(open val sender: Contact, val message: Message) : Event(), CancelEvent {
    override fun cancelAble() = true
}

open class GroupMessageEvent(override val sender: Member, val group: Group, message: Message) : MessageEvent(sender, message)
open class PrivateMessageEvent(sender: Contact, message: Message) : MessageEvent(sender, message)

open class MessageRecallEvent(val sender: Contact, val operator: Contact, val messageId: Int) : Event()
open class PrivateRecallEvent(sender: Contact, operator: Contact, messageId: Int) : MessageRecallEvent(sender, operator, messageId)
open class GroupRecallEvent(val group: Group, sender: Member, operator: Member, messageId: Int) : MessageRecallEvent(sender, operator, messageId)

open class FriendListEvent : Event()
open class FriendAddEvent(val friend: Friend) : FriendListEvent()
open class FriendDeleteEvent(val friend: Friend) : FriendListEvent()

open class GroupListEvent : Event()
open class BotJoinGroupEvent(val group: Group) : GroupListEvent()
open class BotLevelGroupEvent(val group: Group) : GroupListEvent() {
    open class Leave(group: Group) : BotLevelGroupEvent(group)
    open class Kick(val operator: Member) : BotLevelGroupEvent(operator.group)
}

open class NewRequestEvent(val message: String) : Event(), CancelEvent {
    override fun cancelAble() = true
    var accept: Boolean? = null
}

open class NewFriendRequestEvent(val qq: UserInfo,val group: Group?, message: String) : NewRequestEvent(message)
open class GroupInviteEvent(val group: GroupInfo, val qq: UserInfo, message: String) : NewRequestEvent(message)
open class GroupMemberRequestEvent(val group: Group, val qq: UserInfo, message: String) : NewRequestEvent(message), CancelEvent {
    override fun cancelAble() = true
    val blackList = false
}

open class GroupMemberEvent(val group: Group, val member: Member) : Event()
open class GroupMemberJoinEvent(group: Group, member: Member) : GroupMemberEvent(group, member) {
    open class Join(group: Group, member: Member) : GroupMemberJoinEvent(group, member)
    open class Invite(group: Group, member: Member, val inviter: Member) : GroupMemberJoinEvent(group, member)
}
@Deprecated("Image 创建 API 调整，使得命名语义更加清晰。", ReplaceWith("GroupMemberJoinEvent.Invite"))
open class GroupMemberInviteEvent(group: Group, member: Member, inviter: Member) : GroupMemberJoinEvent.Invite(group, member, inviter)

open class GroupMemberLeaveEvent(group: Group, member: Member) : GroupMemberEvent(group, member){
    open class Leave(group: Group, member: Member) : GroupMemberJoinEvent(group, member)
    open class Kick(group: Group, member: Member, val operator: Member) : GroupMemberJoinEvent(group, member)
}
open class GroupMemberKickEvent(group: Group, member: Member, val operator: Member) : GroupMemberEvent(group, member)

open class GroupBanMemberEvent(group: Group, member: Member, val operator: Member, val time: Int) : GroupMemberEvent(group, member)
open class GroupUnBanMemberEvent(group: Group, member: Member, val operator: Member) : GroupMemberEvent(group, member)
open class GroupBanBotEvent(group: Group, member: Member, val operator: Member, val time: Int) : GroupMemberEvent(group, member)
open class GroupUnBanBotEvent(group: Group, member: Member, val operator: Member) : GroupMemberEvent(group, member)

open class ContextSessionCreateEvent(session: ContextSession) : Event()
open class ActionContextInvokeEvent(val context: BotActionContext) : Event(), CancelEvent {
    override fun cancelAble() = true
    open class Per(context: BotActionContext) : ActionContextInvokeEvent(context) {

    }

    open class Post(context: BotActionContext) : ActionContextInvokeEvent(context) {
    }
}

open class SendMessageEvent(val sendTo: Contact, val message: Message):Event() {
    open class Per(sendTo: Contact, message: Message) : SendMessageEvent(sendTo, message), CancelEvent {
        override fun cancelAble() = true
    }
    open class Post(sendTo: Contact, message: Message, messageSource: MessageSource) : SendMessageEvent(sendTo, message)
}