package com.icecreamqaq.yuq.devtools

import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.contact.Account
import com.icecreamqaq.yuq.contact.UserListImpl
import com.icecreamqaq.yuq.devtools.contact.DevFriendList
import com.icecreamqaq.yuq.devtools.contact.DevGroupList

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
    override fun login() {
        TODO("Not yet implemented")
    }


    override fun close() {
        if (online) {
            online = false
        }
    }
}