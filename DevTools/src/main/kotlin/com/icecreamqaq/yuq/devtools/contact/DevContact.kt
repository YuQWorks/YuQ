package com.icecreamqaq.yuq.devtools.contact

import com.icecreamqaq.yuq.contact.Contact
import com.icecreamqaq.yuq.devtools.DevBot
import com.icecreamqaq.yuq.message.Image
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import java.io.File

abstract class DevContact(
    override val bot: DevBot,
    final override val id: Long,
    override val nickname: String
) : Contact {

    override val platformId: String = id.toString()

    override val avatar: String
        get() = "https://q1.qlogo.cn/g?b=qq&nk=$id&s=640"

    override fun sendMessage(message: Message): MessageSource {
        TODO("Not yet implemented")
    }

    override fun uploadImage(imageFile: File): Image {
        TODO("Not yet implemented")
    }

    override fun sendFile(file: File) {
        TODO("Not yet implemented")
    }

}