package com.icecreamqaq.yuq.devtools

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.contact.Account
import com.icecreamqaq.yuq.devtools.contact.DevFriendList
import com.icecreamqaq.yuq.devtools.contact.DevGroupList

class DevBot(
    override val botInfo: Account,
    override val friends: DevFriendList,
    override val groups: DevGroupList,
    override val guilds: GuildList,
): Bot {

    override val platform: String
        get() = "qq"

    override fun refreshFriends(): FriendList = friends

    override fun refreshGroups(): GroupList = groups

    override fun refreshGuilds(): GuildList = guilds

    override fun id2platformId(id: Long): String  = id.toString()

    override fun platformId2id(platformId: String): Long = platformId.toLong()

    override val cookieEx: YuQ.QQCookie
        get() = TODO("Not yet implemented")
    override val web: Web
        get() = TODO("Not yet implemented")

    override fun login() {

    }

    override fun close() {

    }
}