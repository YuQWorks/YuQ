package com.icecreamqaq.yuq.message

import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.icecreamqaq.yuq.entity.Member
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@AutoBind
/***
 * MessageItemFactory 为具体的消息体工厂。
 * @author IceCream
 */
interface MessageItemFactory {

    /***
     * 创建一段纯文本消息体。
     * @param text 纯文本内容。
     */
    fun text(text: String): Text

    /***
     * 创建一个 At 内容
     * @param qq 欲 At 的目标 QQ 号码。
     */
    fun at(qq: Long): At

    /***
     * 创建一个 At 内容
     * @param member 欲 At 的目标 QQ 号码。
     */
    fun at(member: Member): At

    /***
     * 创建一个基础的 QQ 表情。
     * @param id 表情Id。
     */
    fun face(id: Int): Face

    /***
     * 发送一个图片。
     * @param file 图片的位置（File 对象）。
     */
    @Deprecated("Image 创建 API 调整，使得命名语义更加清晰。", ReplaceWith("imageByFile(file)"))
    fun image(file: File) = imageByFile(file)
    fun imageByFile(file: File): Image

    /***
     * 发送一个网络图片
     * @param url 图片的下载地址。
     */
    @Deprecated("Image 创建 API 调整，使得命名语义更加清晰。", ReplaceWith("imageByUrl(url)"))
    fun image(url: String) = imageByUrl(url)
    fun imageByUrl(url: String): Image

    fun imageById(id: String): Image

    fun imageByBufferedImage(bufferedImage: BufferedImage): Image

    fun imageByInputStream(inputStream: InputStream): Image

    fun imageByByteArray(byteArray: ByteArray) = imageByInputStream(byteArray.inputStream())

    fun imageToFlash(image: Image): FlashImage

    /***
     * 发送一段语音
     * @param file 语音的位置（File 对象）。
     */
    @Deprecated("Voice 创建 API 调整，使得命名语义更加清晰。", ReplaceWith("voiceByFile(file)"))
    fun voice(file: File) = voiceByFile(file)
    fun voiceByFile(file: File) = voiceByInputStream(FileInputStream(file))
    fun voiceByInputStream(inputStream: InputStream): Voice
    fun voiceByByteArray(byteArray: ByteArray) = voiceByInputStream(byteArray.inputStream())

    /***
     * 发送一个 Xml 消息（卡片消息）
     * @param serviceId serviceId
     * @param value 具体的 xml 内容。
     */
    fun xmlEx(serviceId: Int, value: String): XmlEx

    /***
     * 发送一个 Json 消息。
     * @param value Json 文本。
     */
    fun jsonEx(value: String): JsonEx

}