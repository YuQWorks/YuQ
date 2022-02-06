package test.yuq.contcat

import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.event.BotLeaveGroupEvent
import com.icecreamqaq.yuq.event.GroupMemberLeaveEvent
import com.icecreamqaq.yuq.message.Image
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.mif
import com.icecreamqaq.yuq.post
import org.slf4j.LoggerFactory
import test.yuq.TestBot
import test.yuq.message.MessageSourceImpl
import java.io.File

abstract class ContactImpl(override val id: Long) : Contact {

    override val platformId: String = id.toString()

    companion object {
        private val log = LoggerFactory.getLogger(ContactImpl::class.java)
    }

    override val yuq = com.icecreamqaq.yuq.yuq

    override fun sendMessage(message: Message): MessageSource {
        return (yuq as TestBot).rainBot.sendMessage(message,this, null) {
            MessageSourceImpl(id)
        }
    }

    override fun sendFile(file: File) {
        println("向 $this 发送文件: ${file.absoluteFile}。")
    }

    override fun uploadImage(imageFile: File): Image {
        println("向 $this 上传图片: ${imageFile.absoluteFile}。")
        return mif.imageByFile(imageFile)
    }
}

class FriendImpl(
    id: Long,
    override val name: String
) : ContactImpl(id), Friend {

    override val avatar: String = ""
    override val guid = id.toString()

    override fun click() {
        println("向 $this 发送戳一戳。")
    }

    override fun delete() {
        println("删除好友 $this。")
    }

    override fun toString() = "Friend($name($id))"

}

class GroupImpl(id: Long, override val name: String, override val maxCount: Int) : ContactImpl(id), Group {
    override val admins = arrayListOf<GroupMemberImpl>()
    override val guid = "g$id"

    override val avatar: String = ""

    override val notices: GroupNoticeList = GroupNoticeList(this)
    override lateinit var owner: Member

    override operator fun get(qq: Long) = super.get(qq) as GroupMemberImpl

    override val members: MutableMap<Long, GroupMemberImpl> = hashMapOf()
    override lateinit var bot: GroupMemberImpl


    override fun leave() {
        println("退出群 $this。")
        BotLeaveGroupEvent.Leave(this).post()
    }

    override fun isFriend() = false

    override fun toString(): String {
        return "Group($name($id))"
    }

    override fun banAll() {
        println("打开群 $this 全员禁言。")
    }

    override fun unBanAll() {
        println("关闭群 $this 全员禁言。")
    }


}

class GroupMemberImpl(
    override val group: GroupImpl,
    id: Long,
    override var name: String,
    override var nameCard: String,
    override var title: String,
    override var permission: Int,
    override var ban: Int,
    override var lastMessageTime: Long,
) : ContactImpl(id), Member {


    override fun at() = mif.at(this)


    override fun ban(time: Int) {
        println("禁言 $this $time 秒。")
        ban = (System.currentTimeMillis() / 1000).toInt() + time
    }

    override fun click() {
        println("向 $this 发送群内戳一戳。")
    }

    override fun clickWithTemp() {
        TODO("Not yet implemented")
    }

    override fun unBan() {
        println("解除禁言 $this。")
        ban = 0
    }

    override fun kick(message: String) {
        println("将 $this 移除群。")
        group.members.remove(id)
        GroupMemberLeaveEvent.Kick(group, this, group.bot).post()
    }

    override fun toString(): String {
        return "Member($nameCard($id)[${group.name}(${group.id}])"
    }

    override val avatar = ""
    override val guid = "${group.id}_$id"
}