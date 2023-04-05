package com.icecreamqaq.yuq.contact

import com.icecreamqaq.yuq.GroupMemberList
import com.icecreamqaq.yuq.error.PermissionDeniedException

interface Group : Contact {

    // 群成员列表，该列表中不包含机器人自己。
    val members: GroupMemberList
    // 群最大成员数量
    val maxCount: Int

    // 机器人在本群的成员对象
    val botMember: GroupMember

    // 群主
    val owner: GroupMember
    // 管理员列表
    val admins: List<GroupMember>

    /*** 群公告列表
     * 向列表中新增/移除对象会实时同步操作。
     */
    val notices: GroupNoticeList

    override fun canSendMessage() = !botMember.isBan()

    // 获取群成员，当群成员不存在时抛出异常。
    operator fun get(qq: Long) = getOrNull(qq) ?: error("Member $qq Not Found!")
    // 获取群成员，当群成员不存在时返回 null。
    fun getOrNull(qq: Long): GroupMember? = if (qq == botMember.id) botMember else members[qq]

    /*** 离开本群
     * 当机器人为群主的时候将解散群聊。
     */
    fun leave()

    /*** 打开全体禁言
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    fun banAll()

    /*** 关闭全体禁言
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    fun unBanAll()

}