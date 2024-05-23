package yuq.qq.contact

import yuq.contact.Account
import yuq.contact.Friend
import yuq.contact.Group
import yuq.contact.GroupMember

interface QAccount : Account {
    // 实际的 QQ 号码
    val platformId: Long
}

interface QFriend : Friend, QAccount
interface QGroup : Group, QAccount
interface QGroupMember : GroupMember, QAccount {
    override val group: QGroup
}