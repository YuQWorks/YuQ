package test.yuq.message

import com.icecreamqaq.yuq.contact.Member
import com.icecreamqaq.yuq.message.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream

class MessageItemFactoryImpl : MessageItemFactory {
    override fun text(text: String) = TextImpl(text)

    override fun at(member: Member) = AtMemberImpl(member)

    override fun at(qq: Long) = AtImpl(qq)

    override fun face(id: Int) = FaceImpl(id)

    override fun imageByBufferedImage(bufferedImage: BufferedImage) = ImageReceive("bufferedImage", "")

    override fun imageByFile(file: File) = ImageReceive(file.absolutePath, "")

    override fun imageById(id: String) = ImageReceive(id, "")

    override fun imageByInputStream(inputStream: InputStream) = ImageReceive("inputStream", "")

    override fun imageByUrl(url: String) = ImageReceive(url, "")

    override fun imageToFlash(image: Image) = FlashImageImpl(image)

    override fun voiceByInputStream(inputStream: InputStream) = TODO()

    override fun xmlEx(serviceId: Int, value: String): XmlEx = XmlImpl(serviceId, value)

    override fun jsonEx(value: String) = JsonImpl(value)

    override fun messagePackage(flag: Int, body: MutableList<IMessageItemChain>) = MessagePackageImpl(flag, body)
}