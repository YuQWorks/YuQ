package com.icecreamqaq.yuq.entity

import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.yuq

interface Contact {
    val id: Long
    val avatar: String
    val name: String

    fun sendMessage(message: Message): MessageSource

    fun convertMessage(message: Message): Message

}

interface User {
    val id: Long
    val avatar: String
    val name: String

    fun isFriend(): Boolean
    fun canSendMessage(): Boolean
}

interface Friend : Contact, User {

    override fun convertMessage(message: Message): Message {
        message.temp = false
        message.qq = id
        return message
    }

    override fun isFriend() = true
    override fun canSendMessage() = true

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

    override fun isFriend() = true
    override fun canSendMessage() = true

    override fun convertMessage(message: Message): Message {
        message.temp = true
        message.group = group.id
        message.qq = id
        return message
    }

    fun isAdmin() = permission > 0
    fun isOwner() = permission == 2

    fun kick(message: String = "")

}