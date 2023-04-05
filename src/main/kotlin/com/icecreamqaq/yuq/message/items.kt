package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.contact.GroupMember
import com.icecreamqaq.yuq.mif


abstract class MessageItemBase : MessageItem, SendAble {

    override operator fun plus(item: MessageItem): MessageItemChain = toItemChain() + item
    override operator fun plus(item: String): MessageItemChain = toItemChain() + item
    override operator fun plus(item: Message): MessageItemChain = toItemChain() + item
    override fun plus(item: MessageItemChain): MessageItemChain = item.unshift(this)
    override fun toMessage(): Message = this.toItemChain().toMessage()

    override fun toItemChain() = MessageItemChain().append(this)

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is MessageItem) return false
        return equal(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString() = logString
}

interface MessageItem : MessagePlus, SendAble {

    val logString: String

    fun toItemChain(): MessageItemChain
    fun equal(other: MessageItem): Boolean
}

interface Text : MessageItem {
    val text: String

    override fun equal(other: MessageItem): Boolean {
        if (other !is Text) return false
        return text == other.text
    }

    companion object {
        fun String.toText() = mif.text(this)
    }
}

interface At : MessageItem {
    val user: Long

    override fun equal(other: MessageItem): Boolean {
        if (other !is At) return false
        return user == other.user
    }
}

interface AtByMember : At {
    val member: GroupMember
    override val user: Long
        get() = member.id

    override val logString: String
        get() = "@${member.nameCardOrName()}($user)"
}

interface Face : MessageItem {
    val faceId: Int

    override fun equal(other: MessageItem): Boolean {
        if (other !is Face) return false
        return faceId == other.faceId
    }
}

interface Image : MessageItem {
    val id: String
    val url: String

    override fun equal(other: MessageItem): Boolean {
        if (other !is Image) return false
        return id == other.id
    }

    companion object {
        fun Image.toFlash() = mif.imageToFlash(this)
    }
}

interface FlashImage : Image {
    val image: Image

    override val id: String
        get() = image.id
    override val url: String
        get() = image.url
}

interface XmlEx : MessageItem {
    val serviceId: Int
    val value: String

    override fun equal(other: MessageItem): Boolean {
        if (other !is XmlEx) return false
        return value == other.value && serviceId == other.serviceId
    }
}

interface JsonEx : MessageItem {
    val value: String

    override fun equal(other: MessageItem): Boolean {
        if (other !is JsonEx) return false
        return value == other.value
    }
}

interface Voice : MessageItem {
    val id: String
    val url: String

    override fun equal(other: MessageItem): Boolean {
        if (other !is Voice) return false
        return id == other.id
    }
}

interface NoImplItem : MessageItem {
    val source: Any

    override fun equal(other: MessageItem): Boolean {
        if (other !is NoImplItem) return false
        return source == other.source
    }
}
