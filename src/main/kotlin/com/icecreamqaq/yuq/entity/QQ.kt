package com.icecreamqaq.yuq.entity

import com.icecreamqaq.yuq.message.At
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource

interface Contact : User {

    fun sendMessage(message: Message): MessageSource

    fun convertMessage(message: Message): Message

    fun toLogString(): String
    override fun canSendMessage() = true
}

interface User {
    val id: Long
    val avatar: String
    val name: String

    fun isFriend(): Boolean
    fun canSendMessage(): Boolean
}

interface Friend : Contact {

    override fun convertMessage(message: Message): Message {
        message.temp = false
        message.qq = id
        return message
    }

    override fun isFriend() = true
    override fun canSendMessage() = true

    override fun toLogString() = "$name($id)"

    fun delete()
}

interface Group : Contact {

    val members: Map<Long, Member>
    val bot: Member

    operator fun get(qq: Long): Member {
        return members[qq] ?: error("Member $qq Not Found!")
    }

    override fun convertMessage(message: Message): Message {
        message.temp = false
        message.group = id
        return message
    }

    fun leave()
    fun banAll()
    fun unBanAll()

    override fun toLogString() = "$name($id)"

}

interface Member : Contact, User {

    val group: Group
    val permission: Int

    var nameCard: String
    val title: String

    val ban: Int
    fun isBan() = ban > 0
    fun ban(time: Int)
    fun unBan()
    fun nameCardOrName() = if (nameCard == "") name else nameCard

    override fun isFriend() = true
    override fun canSendMessage() = true

    override fun toLogString() = "${nameCardOrName()}($id)[${group.name}(${group.id})]"
    fun toLogStringSingle() = "${nameCardOrName()}($id)"

    override fun convertMessage(message: Message): Message {
        message.temp = true
        message.group = group.id
        message.qq = id
        return message
    }

    fun at(): At

    fun isAdmin() = permission > 0
    fun isOwner() = permission == 2

    fun kick(message: String = "")

}