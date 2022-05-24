package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.yuq

interface MessageSource {
    val id: Int
    val sender: Long
    val sendTime: Long
    val sendTo: Long
    val liteMsg: String

    fun recall(): Int
}

interface FriendMessageSource : MessageSource {

}

interface GroupMessageSource : MessageSource {
    val groupCode: Long
}

interface TempMessageSource : MessageSource {
    val groupCode: Long
}

interface GuildMessageSource : MessageSource {
    val guildId: Long
    val channelId: Long
}


data class MessageFailByCancel(
    override val id: Int,
    override val sender: Long,
    override val sendTime: Long,
    override val sendTo: Long,
    override val liteMsg: String,
    override val groupCode: Long,
    override val guildId: Long,
    override val channelId: Long,
) : FriendMessageSource, GroupMessageSource, TempMessageSource, GuildMessageSource {
    override fun recall(): Int {
        return 0
    }

    constructor(contact: Friend, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        -1,
        -1,
        -1
    )

    constructor(contact: Group, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        contact.id,
        -1,
        -1
    )

    constructor(contact: Member, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        contact.group.id,
        -1,
        -1
    )

    constructor(contact: Channel, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        -1,
        contact.guild.id,
        contact.id
    )

    companion object {
        fun create(contact: Contact, liteMsg: String): MessageFailByCancel =
            when(contact){
                is Friend -> MessageFailByCancel(contact, liteMsg)
                is Group -> MessageFailByCancel(contact, liteMsg)
                is Member -> MessageFailByCancel(contact, liteMsg)
                is Channel -> MessageFailByCancel(contact, liteMsg)
                else -> error("联系人 $contact 可能无法创建消息。")
            }

    }
}

data class MessageFailByReadTimeOut(
    override val id: Int,
    override val sender: Long,
    override val sendTime: Long,
    override val sendTo: Long,
    override val liteMsg: String,
    override val groupCode: Long,
    override val guildId: Long,
    override val channelId: Long,
) : FriendMessageSource, GroupMessageSource, TempMessageSource, GuildMessageSource {
    override fun recall(): Int {
        return 0
    }

    constructor(contact: Friend, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        -1,
        -1,
        -1
    )

    constructor(contact: Group, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        contact.id,
        -1,
        -1
    )

    constructor(contact: Member, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        contact.group.id,
        -1,
        -1
    )

    constructor(contact: Channel, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        -1,
        contact.guild.id,
        contact.id
    )

    companion object {
        fun create(contact: Contact, liteMsg: String): MessageFailByReadTimeOut =
            when(contact){
                is Friend -> MessageFailByReadTimeOut(contact, liteMsg)
                is Group -> MessageFailByReadTimeOut(contact, liteMsg)
                is Member -> MessageFailByReadTimeOut(contact, liteMsg)
                is Channel -> MessageFailByReadTimeOut(contact, liteMsg)
                else -> error("联系人 $contact 可能无法创建消息。")
            }

    }
}

data class FakeMessageSource(
    override val id: Int,
    override val sender: Long,
    override val sendTime: Long,
    override val sendTo: Long,
    override val liteMsg: String,
    override val groupCode: Long,
    override val guildId: Long,
    override val channelId: Long,
) : FriendMessageSource, GroupMessageSource, TempMessageSource, GuildMessageSource {
    override fun recall(): Int {
        return 0
    }

    constructor(contact: Friend, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        -1,
        -1,
        -1
    )

    constructor(contact: Group, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        contact.id,
        -1,
        -1
    )

    constructor(contact: Member, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        contact.group.id,
        -1,
        -1
    )

    constructor(contact: Channel, liteMsg: String) : this(
        -1,
        yuq.botId,
        System.currentTimeMillis(),
        contact.id,
        liteMsg,
        -1,
        contact.guild.id,
        contact.id
    )

    companion object {
        fun create(contact: Contact, liteMsg: String): FakeMessageSource =
            when(contact){
                is Friend -> FakeMessageSource(contact, liteMsg)
                is Group -> FakeMessageSource(contact, liteMsg)
                is Member -> FakeMessageSource(contact, liteMsg)
                is Channel -> FakeMessageSource(contact, liteMsg)
                else -> error("联系人 $contact 可能无法创建消息。")
            }

    }
}