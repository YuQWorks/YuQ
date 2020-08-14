package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.entity.Contact


abstract class MessageItemBase : MessageItem {
    override operator fun plus(item: MessageItem): Message = toMessage() + item
    override operator fun plus(item: String): Message = toMessage() + item
    override operator fun plus(item: Message): Message = toMessage() + item
    override fun toMessage(): Message = Message() + this

    override fun equals(other: Any?): Boolean {
        if (other == null)return false
        if (this === other)return true
        if (other !is MessageItem)return false
        return equal(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

interface MessageItem : MessagePlus {
    fun toLocal(contact: Contact): Any
    fun toPath(): String
    fun convertByPathVar(type: PathVar.Type): Any?

    fun toMessage(): Message
    fun toLogString(): String = toPath()

    fun equal(other: MessageItem): Boolean
}

interface Text : MessageItem {
    val text: String

    override fun toPath() = text
    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.Source -> this
        PathVar.Type.String -> text
        PathVar.Type.Switch -> {
            val textLow = text.toLowerCase()
            textLow.contains("true") || text.contains("开") || text.contains("启")
                    || textLow.contains("open") || textLow.contains("enable")
                    || textLow.contains("on") || text.contains("是")
        }
        PathVar.Type.Integer -> text.toInt()
        PathVar.Type.Long -> text.toLong()
        PathVar.Type.Double -> text.toDouble()
        else -> null
    }

    override fun toLogString() = "\"" + text.replace("\n", "\\n") + "\""

    override fun equal(other: MessageItem): Boolean {
        if (other !is Text) return true
        return text == other.text
    }

}

interface At : MessageItem {
    val user: Long

    override fun toPath() = "At_$user"
    override fun convertByPathVar(type: PathVar.Type) = when (type) {
        PathVar.Type.String -> user.toString()
        PathVar.Type.Long -> user
        PathVar.Type.Double -> user.toDouble()
        else -> null
    }

    override fun equal(other: MessageItem): Boolean {
        if (other !is At) return true
        return user == other.user
    }
}

interface Face : MessageItem {
    val faceId: Int

    override fun toPath() = "表情_$faceId"
    override fun convertByPathVar(type: PathVar.Type) = when (type) {
        PathVar.Type.String -> "表情_$faceId"
        PathVar.Type.Integer -> faceId
        PathVar.Type.Long -> faceId.toLong()
        PathVar.Type.Double -> faceId.toDouble()
        else -> null
    }

    override fun equal(other: MessageItem): Boolean {
        if (other !is Face) return false
        return faceId == other.faceId
    }
}

interface Image : MessageItem {
    val id: String
    val url: String

    override fun toPath(): String {
        return "img_$id"
    }

    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.String -> "图片"
        PathVar.Type.Source -> this
        else -> null
    }

    override fun equal(other: MessageItem): Boolean {
        if (other !is Image) return false
        return id == other.id
    }
}

interface FlashImage : Image{
    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.String -> "闪照"
        PathVar.Type.Source -> this
        else -> null
    }
}

interface XmlEx : MessageItem {
    val serviceId: Int
    val value: String

    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.String -> value
        PathVar.Type.Source -> this
        else -> null
    }

    override fun toPath() = "XmlMsg"
    override fun equal(other: MessageItem): Boolean {
        if (other !is XmlEx)return true
        return value == other.value && serviceId == other.serviceId
    }
}

interface JsonEx : MessageItem {
    val value: String

    override fun convertByPathVar(type: PathVar.Type): Any? = when (type) {
        PathVar.Type.String -> value
        PathVar.Type.Source -> this
        else -> null
    }
    override fun toPath() = "JsonMsg"

    override fun equal(other: MessageItem): Boolean {
        if (other !is JsonEx)return true
        return value == other.value
    }
}

interface Voice : MessageItem {
    val url: String
}

interface NoImplItem : MessageItem {
    val source: Any

    override fun toPath() = "NoImpl"
    override fun convertByPathVar(type: PathVar.Type) = "NotImpl"

    override fun equal(other: MessageItem): Boolean {
        if (other !is NoImplItem)return true
        return source == other.source
    }
}
