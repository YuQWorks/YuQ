package yuq.contact

import yuq.Bot
import yuq.annotation.Internal
import yuq.annotation.NoRecommendation
import yuq.message.Message
import yuq.message.MessageSource
import yuq.message.SendAble
import yuq.message.items.Image
import yuq.message.items.Text
import java.io.File

/*** 联系人
 * 该对象描述一个标准联系人，联系人可以用来发送消息。
 * 联系人可能是实际存在的好友，群，群成员，也可能是来自陌生人，添加好友临时会话时的。
 *
 * 联系人对象每个 Bot 唯一，可以保存。
 */
interface Contact : Account {

    // 该联系人所属的机器人
    val bot: Bot

    // YuQ 框架内对联系人的唯一识别码，全局唯一
    val guid: String

    // 获取联系人的上下文会话
    val session: ContactSession

    // 通过 Message 发送一条消息
    fun sendMessage(message: Message): MessageSource

    // 通过 SendAble 发送一条消息
    fun sendMessage(message: SendAble): MessageSource = sendMessage(message.toMessage())

    // 通过 String 发送一条消息
    fun sendMessage(message: String): MessageSource = sendMessage(Text(message))

    // 上传一张图片，返回 Image(MessageItem) 对象。
    fun uploadImage(imageFile: File): Image

    // 发送文件，当 Contact 为 Group 时，表现为上传文件。
    fun sendFile(file: File)

    // 当前联系人是否能发送消息
    fun canSendMessage(): Boolean = true

    // 用于描述输出到日志中的内容。
    @Internal
    @NoRecommendation
    val logString: String
}