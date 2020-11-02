package com.icecreamqaq.yuq.entity

import com.icecreamqaq.yuq.message.At
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.mif
import com.icecreamqaq.yuq.yuq

interface Contact : User {

    fun sendMessage(message: Message): MessageSource

//    fun convertMessage(message: Message): Message

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

//    override fun convertMessage(message: Message): Message {
//        message.temp = false
//        message.qq = id
//        return message
//    }

    override fun isFriend() = true
    override fun canSendMessage() = true

    override fun toLogString() = "$name($id)"

    fun delete()
    fun click()
}

interface Group : Contact {

    val members: Map<Long, Member>
    val bot: Member
    val maxCount: Int

    val owner: Member
    val admins: List<Member>

    operator fun get(qq: Long): Member {
        return members[qq] ?: error("Member $qq Not Found!")
    }

//    override fun convertMessage(message: Message): Message {
//        message.temp = false
//        message.group = id
//        return message
//    }

    /***
     * 离开本群，当机器人是群主的时候解析为解散。
     */
    fun leave()
    fun banAll()
    fun unBanAll()
//    fun getMaxMemberCount(): Int

    override fun toLogString() = "$name($id)"

}

interface Member : Contact, User {

    val group: Group
    val permission: Int

    var nameCard: String
    val title: String

    val ban: Int
    fun isBan() = ban > (System.currentTimeMillis() / 1000).toInt()
    fun ban(time: Int)
    fun unBan()
    fun nameCardOrName() = if (nameCard == "") name else nameCard

    fun click()
    fun clickWithTemp()

    override fun isFriend() = true
    override fun canSendMessage() = true

    override fun toLogString() = "${nameCardOrName()}($id)[${group.name}(${group.id})]"
    fun toLogStringSingle() = "${nameCardOrName()}($id)"

//    override fun convertMessage(message: Message): Message {
//        message.temp = true
//        message.group = group.id
//        message.qq = id
//        return message
//    }

    fun at(): At = mif.at(this)

    fun isAdmin() = permission > 0
    fun isOwner() = permission == 2

    fun kick(message: String = "")

    companion object {
        fun Member.toFriend(): Friend? = yuq.friends[id]
    }

}

interface AnonymousMember : Member {

    override fun canSendMessage() = false
    override fun isFriend() = false

}

enum class UserSex {
    man, woman, none
}

data class UserInfo(
        override val id: Long,
        override val avatar: String,
        override val name: String,
        val sex: UserSex,
        val age: Int,
        val qqAge: Int,
        val level: Int,
        val loginDays: Int,

        val vips: List<UserVip>
) : User {
    override fun isFriend() = yuq.friends.containsKey(id)

    override fun canSendMessage() = false
}

data class GroupInfo(
        val id: Long,
        val name: String,
        val maxCount: Int,

        val owner: User,
        val admin: List<User>
)

//interface UserInfo : User {
//
//    val sex: String
//    val age: Int
//    val qAge: Int
//
//    val level: Int
//
//    val loginDays: Int
//
//
//}

data class UserVip(
        val id: Int,
        val name: Int,
        val ipSuper: Int,
        val desc: String,
        val level: Int,
        val yearFlag: Boolean,
        val para: String
)