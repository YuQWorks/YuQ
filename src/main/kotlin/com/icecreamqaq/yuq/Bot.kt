package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.entity.User

interface Bot {

    // 机器人运行的平台
    val platform: String

    /***
     * 机器人的 QQ 号码
     */
    val botId: Long
        get() = botInfo.id

    /***
     * 机器人的个人信息
     */
    val botInfo: User

    /***
     * 好友列表
     */
    val friends: FriendList

    /***
     * 群列表
     */
    val groups: GroupList

    /***
     * 频道列表
     */
    val guilds: GuildList

    /***
     * 刷新好友列表
     */
    fun refreshFriends(): FriendList

    /***
     * 刷新群列表
     */
    fun refreshGroups(): GroupList

    /***
     * 刷新频道列表
     */
    fun refreshGuilds(): GuildList

    // 通过 ID 获取 Platform ID。
    fun id2platformId(id: Long): String

    // 通过 Platform ID 获取 ID。
    fun platformId2id(platformId: String): Long

    val cookieEx: YuQ.QQCookie
    val web: Web


}