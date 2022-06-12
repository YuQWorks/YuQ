package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.entity.User
import com.icecreamqaq.yuq.message.MessageItemFactory

interface YuQ : Bot {

    val messageItemFactory: MessageItemFactory

    val bots: List<Bot>

    fun findByPlatformAndPlatformId(platform: String, platformId: String): Bot? =
        bots.firstOrNull { it.platform == platform && it.botInfo.platformId == platformId }

    override val platform: String
        get() = bots[0].platform

    /*** 机器人的 QQ 号码
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override val botId: Long
        get() = bots[0].botId


    /*** 机器人的个人信息
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override val botInfo: User
        get() = bots[0].botInfo

    /*** 好友列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override val friends: FriendList
        get() = bots[0].friends

    /*** 群列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override val groups: GroupList
        get() = bots[0].groups

    /*** 频道列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override val guilds: GuildList
        get() = bots[0].guilds

    /*** 刷新好友列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override fun refreshFriends(): FriendList =
        bots[0].refreshFriends()

    /*** 刷新群列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override fun refreshGroups(): GroupList =
        bots[0].refreshGroups()

    /*** 刷新频道列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override fun refreshGuilds(): GuildList =
        bots[0].refreshGuilds()

    /*** 通过 ID 获取 Platform ID。
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override fun id2platformId(id: Long): String =
        bots[0].id2platformId(id)

    /*** 通过 Platform ID 获取 ID。
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */

    override fun platformId2id(platformId: String): Long =
        bots[0].platformId2id(platformId)

    /*** 获取 Cookie 信息
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override val cookieEx: QQCookie
        get() = bots[0].cookieEx

    /*** 获取携带 Cookie 的 Web。
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    override val web: Web
        get() = bots[0].web

    interface QQCookie {
        val skey: String
        val gtk: Long
        val pskeyMap: Map<String, Pskey>

        data class Pskey(val pskey: String, val gtk: Long)
    }

}