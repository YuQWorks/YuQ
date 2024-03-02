package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.contact.GroupMember
import rain.function.IO
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import javax.imageio.ImageIO

/*** MessageItemFactory 为具体的消息体工厂。
 * @author IceCream
 */
class MessageItemFactory {

    /*** 创建一段纯文本消息体。
     * @param text 纯文本内容。
     */
    fun text(text: String): Text = Text(text)

    /*** 创建一个 @ 内容
     * @param qq 欲 At 的目标 QQ 号码。
     */
    fun at(qq: Long): At = At(qq)

    /*** 创建一个 @ 内容
     * @param member 欲 At 的目标 QQ 号码。
     */
    fun at(member: GroupMember): At = AtByMember(member)

    /*** 创建一个基础的 QQ 表情。
     * @param id 表情Id。
     */
    fun face(id: Int): Face = Face(id)

    /*** 使用文件发送一个图片。
     * @param file 图片的位置（File 对象）。
     */
    fun imageByFile(file: File): Image = OfflineImage(file)

    /*** 发送一个网络图片
     * @param url 图片的下载地址。
     */
//    fun imageByUrl(url: String): Image = OfflineImage(web.download(url, file = null))


    fun imageByBufferedImage(bufferedImage: BufferedImage, format: String = "PNG"): Image =
        OfflineImage(IO.tmpFile().also { ImageIO.write(bufferedImage, format, it) })

    fun imageByInputStream(inputStream: InputStream): Image =
        OfflineImage(IO.tmpFile().also { IO.copy(inputStream, it.outputStream()) })

    fun imageByByteArray(byteArray: ByteArray) =
        OfflineImage(IO.tmpFile().also { it.writeBytes(byteArray) })

    fun imageToFlash(image: Image): FlashImage =
        FlashImage(image)

    fun imageById(id: String): Image = TODO()


    /*** 发送一段语音
     * @param file 语音的位置（File 对象）。
     */
    fun voiceByFile(file: File): Voice = OfflineVoice(file)
    fun voiceByInputStream(inputStream: InputStream): Voice =
        voiceByFile(IO.tmpFile().also { IO.copy(inputStream, it.outputStream()) })

    fun voiceByByteArray(byteArray: ByteArray) =
        voiceByFile(IO.tmpFile().also { it.writeBytes(byteArray) })

    /*** 发送一个 Xml 消息（卡片消息）
     * @param serviceId serviceId
     * @param value 具体的 xml 内容。
     */
    fun xmlEx(serviceId: Int, value: String): XmlEx = XmlEx(serviceId, value)

    /*** 发送一个 Json 消息。
     * @param value Json 文本。
     */
    fun jsonEx(value: String): JsonEx = JsonEx(value)

//    fun messagePackage(flag: Int, body: MutableList<IMessageItemChain>): MessagePackage

}