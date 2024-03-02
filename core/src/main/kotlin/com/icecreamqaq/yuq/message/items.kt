package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.contact.GroupMember
import com.icecreamqaq.yuq.mif
import java.io.File


class Text(val text: String) : MessageItemBase() {
    override val logString: String
        get() = text

    override fun equal(other: MessageItem): Boolean {
        if (other !is Text) return false
        return text == other.text
    }

    companion object {
        fun String.toText() = mif.text(this)
    }
}

open class At(val user: Long) : MessageItemBase() {

    override val logString: String
        get() = "@$user"

    override fun equal(other: MessageItem): Boolean {
        if (other !is At) return false
        return user == other.user
    }
}

class AtByMember(val member: GroupMember) : At(member.id) {

    override val logString: String
        get() = "@${member.nameCardOrName()}($user)"
}

class Face(val faceId: Int) : MessageItemBase() {

    override val logString: String
        get() = "表情: $faceId"

    override fun equal(other: MessageItem): Boolean {
        if (other !is Face) return false
        return faceId == other.faceId
    }
}

abstract class Image(
    val platform: String,
    val id: String,
    val url: String
) : MessageItemBase() {

    override val logString: String
        get() = "图片: $id"

    override fun equal(other: MessageItem): Boolean {
        if (other !is Image) return false
        return id == other.id
    }

    companion object {
        fun Image.toFlash() = mif.imageToFlash(this)
    }
}

class OnlineImage(platform: String, id: String, url: String) : Image(platform, id, url)
class OfflineImage(val imageFile: File) : Image("universal", imageFile.name, "")

class FlashImage(val image: Image) : Image(image.platform, image.id, image.url) {

    override val logString: String
        get() = "闪照: $id"

}

class XmlEx(
    val serviceId: Int,
    val value: String
) : MessageItemBase() {

    override val logString: String
        get() = "Xml: $serviceId"

    override fun equal(other: MessageItem): Boolean {
        if (other !is XmlEx) return false
        return value == other.value && serviceId == other.serviceId
    }
}

class JsonEx(val value: String) : MessageItemBase() {

    override val logString: String
        get() = "JSON"

    override fun equal(other: MessageItem): Boolean {
        if (other !is JsonEx) return false
        return value == other.value
    }
}



abstract class Voice(
    val platform: String,
    val id: String,
    val url: String
) : MessageItemBase() {

    override val logString: String
        get() = "语音: $id"

    override fun equal(other: MessageItem): Boolean {
        if (other !is Voice) return false
        return id == other.id
    }
}

class OnlineVoice(platform: String, id: String, url: String) : Voice(platform, id, url)
class OfflineVoice(val voiceFile: File) : Voice("universal", voiceFile.name, "")

class NoImplItem(
    val platformIdentifier: String,
    val source: Any
) : MessageItemBase() {

    override val logString: String
        get() = "NotImpl"

    override fun equal(other: MessageItem): Boolean {
        if (other !is NoImplItem) return false
        return source == other.source
    }
}
