package com.icecreamqaq.yuq.contact

import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.botService
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.util.WebHelper.Companion.postWithQQKey
import java.io.File


/*** 匿名群成员对象
 * 匿名群成员对象是一个临时对象，不保证稳定，不保证唯一。
 * 匿名群成员无法用于创建临时会话，也无法发送消息，或发送戳一戳等。
 */
interface AnonymousMember : GroupMember

enum class UserSex {
    MAN, WOMAN, NONE
}

data class UserInfo(
    override val bot: Bot,
    override val id: Long,
    override val platformId: String = id.toString(),
    override val avatar: String,
    override val nickname: String,
    val sex: UserSex,
    val age: Int,
    val qqAge: Int,
    val level: Int,
    val loginDays: Int,
    val vips: List<UserVip>,
) : Account

data class GroupInfo(
    val id: Long,
    val name: String,
    val maxCount: Int,

    val owner: UserInfo,
    val admin: List<UserInfo>
)

open class GroupNotice {

    protected var id: Long? = null
    fun getId() = id ?: -1

    var text: String = ""

    var isTop = false
    var popWindow = false
    var confirmRequired = false

}

abstract class GroupNoticeList(protected val group: Group) {

    internal val noticeList = arrayListOf<GroupNotice>()

    operator fun get(id: Int) {
        TODO("Feature not supported")
    }

    abstract fun add(notice: GroupNotice)

}

data class UserVip(
    val id: Int,
    val name: Int,
    val ipSuper: Int,
    val desc: String,
    val level: Int,
    val yearFlag: Boolean,
    val para: String
)