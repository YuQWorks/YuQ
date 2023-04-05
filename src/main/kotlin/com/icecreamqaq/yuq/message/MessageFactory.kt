package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.contact.Member
import com.icecreamqaq.yuq.message.Message.Companion.toMessageByRainCode
import com.icecreamqaq.yuq.mif
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream

open class MessageLineQ(val message: Message = Message()) : SendAble {
    fun plus(item: MessageItem): MessageLineQ {
        message.plus(item)
        return this
    }

    fun plus(chain: MessageItemChain): MessageLineQ {
        message.plus(chain)
        return this
    }

    fun plus(chain: Message): MessageLineQ {
        message.plus(chain)
        return this
    }

    fun line() = plus(mif.text("\n"))
    fun text(text: String) = plus(mif.text(text))
    fun textLine(text: String) = plus(mif.text("$text\n"))
    fun rainCode(codeString: String) = plus(codeString.toMessageByRainCode())

    fun at(qq: Long) = plus(mif.at(qq))
    fun at(member: Member) = plus(mif.at(member))

    fun face(id: Int) = plus(mif.face(id))

    fun imageByFile(file: File) = plus(mif.imageByFile(file))

    fun imageByUrl(url: String) = plus(mif.imageByUrl(url))

    fun imageById(id: String) = plus(mif.imageById(id))

    fun imageByBufferedImage(bufferedImage: BufferedImage) = plus(mif.imageByBufferedImage(bufferedImage))

    fun imageByInputStream(inputStream: InputStream) = plus(mif.imageByInputStream(inputStream))

    fun imageToFlash(image: Image) = plus(mif.imageToFlash(image))

    fun voiceByInputStream(inputStream: InputStream) = plus(mif.voiceByInputStream(inputStream))

    fun xmlEx(serviceId: Int, value: String) = plus(mif.xmlEx(serviceId, value))

    fun jsonEx(value: String) = plus(mif.jsonEx(value))

    fun recallDelay(time: Long): MessageLineQ {
        message.recallDelay = time
        return this
    }

    override fun toMessage() = message
}

fun buildMessage(body: MessageLineQ.() -> Unit): Message = MessageLineQ().apply(body).message