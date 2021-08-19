package com.icecreamqaq.yuq.entity

import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.annotation.Dev
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.mif
import com.icecreamqaq.yuq.rainBot
import com.icecreamqaq.yuq.util.WebHelper.Companion.postWithQQKey
import com.icecreamqaq.yuq.yuq
import java.io.File

interface Contact : User {

    val yuq: YuQ

    fun sendMessage(message: Message): MessageSource
    fun sendMessage(message: MessageLineQ): MessageSource = sendMessage(message.message)
    fun sendMessage(message: String): MessageSource = sendMessage(message.toMessage())
    fun sendMessage(messageItem: MessageItem): MessageSource = sendMessage(messageItem.toMessage())

    //    fun convertMessage(message: Message): Message
    /***
     * 上传一张图片，返回 Image(MessageItem) 对象。
     */
    fun uploadImage(imageFile: File): Image

    /***
     * 发送文件，当 Contact 为 Group 时，表现为上传文件。
     */
    fun sendFile(file: File)

    fun toLogString(): String
    override fun canSendMessage() = true
}

@Dev
interface ContactUser {

}

//interface BlockFriend{
//    @JvmName("")
//    fun delete()
//}
//interface SuspendFriend{
//    suspend fun delete()
//}
//interface XFriend :BlockFriend,SuspendFriend


interface User {
    val id: Long
    val avatar: String
    val name: String

    fun isFriend(): Boolean
    fun canSendMessage(): Boolean
}

interface Friend : Contact {

//    override fun convertMessage(message: Message): Message {
//        message.temp = false
//        message.qq = id
//        return message
//    }

    override fun isFriend() = true
    override fun canSendMessage() = true

    override fun toLogString() = "$name($id)"

    fun delete()
    fun click()
}

interface Group : Contact {

    val session: ContextSession
        get() = rainBot.getContextSession("g$id")

    val members: Map<Long, Member>
    val bot: Member
    val maxCount: Int

    val owner: Member
    val admins: List<Member>

    val notices: GroupNoticeList

    override fun canSendMessage() = !bot.isBan()

    operator fun get(qq: Long) = getOrNull(qq) ?: error("Member $qq Not Found!")

    fun getOrNull(qq: Long): Member? = members[qq] ?: if (qq == bot.id) bot else null


//    override fun convertMessage(message: Message): Message {
//        message.temp = false
//        message.group = id
//        return message
//    }

    /***
     * 离开本群，当机器人是群主的时候解析为解散。
     */
    fun leave()
    fun banAll()
    fun unBanAll()
//    fun getMaxMemberCount(): Int

    override fun toLogString() = "$name($id)"

}

interface Member : Contact, User {

    val group: Group
    val permission: Int

    var nameCard: String
    var title: String

    val ban: Int
    val lastMessageTime: Long

    fun isBan() = ban > (System.currentTimeMillis() / 1000).toInt()
    fun ban(time: Int)
    fun unBan()
    fun nameCardOrName() = if (nameCard == "") name else nameCard

    fun click()
    fun clickWithTemp()

//    fun lastMessageTime() = -1L

    override fun isFriend() = true
    override fun canSendMessage() = true

    override fun toLogString() = "${nameCardOrName()}($id)[${group.name}(${group.id})]"
    fun toLogStringSingle() = "${nameCardOrName()}($id)"

    fun at(): At = mif.at(this)

    fun isAdmin() = permission > 0
    fun isOwner() = permission == 2

    fun kick() = kick("")
    fun kick(message: String = "")

    companion object {
        fun Member.toFriend(): Friend? = yuq.friends[id]
    }

}

interface AnonymousMember : Member {

    override fun canSendMessage() = false
    override fun isFriend() = false

}

enum class UserSex {
    man, woman, none
}

data class UserInfo(
    override val id: Long,
    override val avatar: String,
    override val name: String,
    val sex: UserSex,
    val age: Int,
    val qqAge: Int,
    val level: Int,
    val loginDays: Int,

    val vips: List<UserVip>
) : User {
    override fun isFriend() = yuq.friends.containsKey(id)

    override fun canSendMessage() = false
}

data class GroupInfo(
    val id: Long,
    val name: String,
    val maxCount: Int,

    val owner: User,
    val admin: List<User>
)

open class GroupNotice {

    protected var id: Long? = null
    fun getId() = id ?: -1

    var text: String = ""

    var isTop = false
    var popWindow = false
    var confirmRequired = false

}

open class GroupNoticeList(protected val group: Group) {

    internal val noticeList = arrayListOf<GroupNotice>()

    operator fun get(id: Int) {
        TODO("Feature not supported")
    }

    fun add(notice: GroupNotice) {
        notice.run {
            yuq.web.postWithQQKey(
                "https://web.qun.qq.com/cgi-bin/announce/add_qun_notice", mutableMapOf(
                    "qid" to group.id.toString(),
                    "bkn" to "{gtk}",
                    "text" to text,
                    "pinned" to if (isTop) "1" else "0",
                    "type" to "1",
                    "settings" to "{\"is_show_edit_card\":0," +
                            "\"tip_window_type\":${if (popWindow) "1" else "0"}," +
                            "\"confirm_required\":${if (confirmRequired) "1" else "0"}}"
                )
            )
        }
    }

}

//interface UserInfo : User {
//
//    val sex: String
//    val age: Int
//    val qAge: Int
//
//    val level: Int
//
//    val loginDays: Int
//
//
//}

data class UserVip(
    val id: Int,
    val name: Int,
    val ipSuper: Int,
    val desc: String,
    val level: Int,
    val yearFlag: Boolean,
    val para: String
)