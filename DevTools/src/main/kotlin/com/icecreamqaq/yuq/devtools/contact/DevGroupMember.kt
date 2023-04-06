package com.icecreamqaq.yuq.devtools.contact

import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.contact.Group
import com.icecreamqaq.yuq.contact.GroupMember
import com.icecreamqaq.yuq.devtools.DevBot
import com.icecreamqaq.yuq.error.PermissionDeniedException
import com.icecreamqaq.yuq.event.GroupMemberEvent
import com.icecreamqaq.yuq.event.GroupMemberLeaveEvent
import com.icecreamqaq.yuq.post

class DevGroupMember(
    bot: DevBot,
    override val group: DevGroup,
    id: Long,
    nickname: String,
    override var namecard: String,
    override val permission: Int,
    override var title: String,
    override var ban: Long,
    override val lastMessageTime: Long
) : DevContact(bot, id, nickname), GroupMember {

    override fun ban(time: Int) {
        if (group.botMember.permission <= permission) throw PermissionDeniedException()
        ban = System.currentTimeMillis() + time * 1000
    }

    override fun unBan() {
        if (group.botMember.permission <= permission) throw PermissionDeniedException()
        ban = 0
    }

    override fun kick(message: String) {
        if (group.botMember.permission <= permission) throw PermissionDeniedException()
        group.members.remove(this)
        GroupMemberLeaveEvent.Kick(group, this, group.botMember).post()
    }

    override val guid: String = "${bot.botId}_g_${group.id}_m_$id"

    override val logStringSingle: String = "${nameCardOrName()}($id)"
    override val logString: String = "${nameCardOrName()}($id)[${group.nickname}(${group.id})]"
}