package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.entity.User
import com.icecreamqaq.yuq.message.MessageItemFactory
import java.security.cert.Extension

interface YuQ {

    val messageItemFactory: MessageItemFactory

    val bots: List<Bot>

    fun findByPlatformAndPlatformId(platform: String, platformId: String): Bot? =
        bots.firstOrNull { it.platform == platform && it.botInfo.platformId == platformId }

    val platform: String
        get() = bots[0].platform

    /*** 机器人的 QQ 号码
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    val botId: Long
        get() = bots[0].botId


    /*** 机器人的个人信息
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    val botInfo: User
        get() = bots[0].botInfo

    /*** 好友列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    val friends: FriendList
        get() = bots[0].friends

    /*** 群列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    val groups: GroupList
        get() = bots[0].groups

    /*** 频道列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    val guilds: GuildList
        get() = bots[0].guilds

    /*** 刷新好友列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    fun refreshFriends(): FriendList =
        bots[0].refreshFriends()

    /*** 刷新群列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    fun refreshGroups(): GroupList =
        bots[0].refreshGroups()

    /*** 刷新频道列表
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
    fun refreshGuilds(): GuildList =
        bots[0].refreshGuilds()

    /*** 通过 ID 获取 Platform ID。
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
     fun id2platformId(id: Long): String =
        bots[0].id2platformId(id)

    /*** 通过 Platform ID 获取 ID。
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */

     fun platformId2id(platformId: String): Long =
        bots[0].platformId2id(platformId)

    /*** 获取 Cookie 信息
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
     val cookieEx: QQCookie
        get() = bots[0].cookieEx

    /*** 获取携带 Cookie 的 Web。
     * YuQ 内实现的一切 Bot 函数都是获取首位 Bot 内值。
     * 单 Bot 用户可正常使用，多 Bot 用户请自己获取目标 Bot。
     */
     val web: Web
        get() = bots[0].web

    interface QQCookie {
        val skey: String
        val gtk: Long
        val pskeyMap: Map<String, Pskey>

        data class Pskey(val pskey: String, val gtk: Long)
    }

    /*** 动态创建 Bot 实例。
     * @param id 登录账户 ID。
     * @param pwd 登录账户密码。
     * @param botName 机器人名
     * @param extData 目标 Runtime 所需要的扩展数据。
     * @return 创建后的机器人对象。
     *
     * 根据目标平台不同，创建 Bot 具体所需要的参数与结果并不相同。
     */
    fun createBot(id: String, pwd: String, botName: String? = null, extData: String? = null): Bot

}