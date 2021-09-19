package com.icecreamqaq.yuq.message

import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.mif
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream

//@AutoBind
//@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
//interface MessageFactory {
//
//    fun newMessage():Message
//    fun newGroup(group:Long):Message
//    fun newPrivate(qq:Long):Message
//    fun newTemp(group:Long,qq:Long):Message
//
//}

//class MessageFactoryImpl : MessageFactory {
//    override fun newMessage(): Message {
//        return Message()
//    }
//
//    override fun newGroup(group: Long): Message {
//        val message = newMessage()
//        message.group = group
//        return message
//    }
//
//    override fun newPrivate(qq: Long): Message {
//        val message = Message()
//        message.qq = qq
//        return message
//    }
//
//    override fun newTemp(group: Long, qq: Long): Message {
//        val message = Message()
//        message.temp = true
//        message.qq = qq
//        message.group = group
//        return message
//    }
//}

open class MessageLineQ(val message: Message = Message()) {
    fun plus(item: MessageItem): MessageLineQ {
        message.plus(item)
        return this
    }

    fun line() = plus(mif.text("\n"))
    fun text(text: String) = plus(mif.text(text))
    fun textLine(text: String) = plus(mif.text("$text\n"))

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
}

fun buildMessage(body: MessageLineQ.() -> Unit): Message = MessageLineQ().apply(body).message