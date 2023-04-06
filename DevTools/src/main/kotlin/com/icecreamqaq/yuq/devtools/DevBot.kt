package com.icecreamqaq.yuq.devtools

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.contact.Account
import com.icecreamqaq.yuq.contact.UserListImpl
import com.icecreamqaq.yuq.devtools.contact.DevFriendList
import com.icecreamqaq.yuq.devtools.contact.DevGroupList
import com.icecreamqaq.yuq.event.BotStatusEvent

class DevBot(
    override val botInfo: Account
): Bot {

    var online = false

    override val platform: String
        get() = "qq"

    override val friends: DevFriendList = DevFriendList()
    override val groups: DevGroupList = DevGroupList()
    override val guilds: GuildList = UserListImpl()

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
        if (online) BotStatusEvent.ReOnline(this).post()
        else {
            online = true
            BotStatusEvent.Online(this).post()
        }
    }

    override fun close() {
        if (online) {
            online = false
            BotStatusEvent.Offline(this).post()
        }
    }
}