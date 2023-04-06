package com.icecreamqaq.yuq.devtools.contact

import com.icecreamqaq.yuq.contact.Group
import com.icecreamqaq.yuq.contact.GroupMember
import com.icecreamqaq.yuq.contact.GroupNoticeList
import com.icecreamqaq.yuq.devtools.DevBot
import com.icecreamqaq.yuq.error.PermissionDeniedException
import com.icecreamqaq.yuq.event.BotLeaveGroupEvent
import com.icecreamqaq.yuq.post

class DevGroup(
    bot: DevBot,
    id: Long,
    nickname: String,
    override val maxCount: Int
) : DevContact(bot, id, nickname), Group {

    override lateinit var botMember: GroupMember

    override val members = DevGroupMemberList()
    override lateinit var owner: GroupMember
    override val admins: List<GroupMember> = ArrayList()
    override val notices: GroupNoticeList
        get() = TODO("Not yet implemented")

    fun initMembers(members: List<DevGroupMember>) {
        for (member in members) {
            if (member.id == bot.botId) botMember = member
            else this.members.add(member)
        }
        if (!::botMember.isInitialized)
            botMember = DevGroupMember(bot, this, bot.botId, bot.botInfo.nickname, "", 0, "", 0, 0)
    }

    override fun leave() {
        bot.groups.remove(this)
        BotLeaveGroupEvent.Leave(this).post()
    }

    override fun banAll() {
        if (!botMember.isAdmin()) throw PermissionDeniedException()
    }

    override fun unBanAll() {
        if (!botMember.isAdmin()) throw PermissionDeniedException()
    }

    override val guid: String = "${bot.botId}_g_$id"
    override val logString: String = "$nickname($id)"
}