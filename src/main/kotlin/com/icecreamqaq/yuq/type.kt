package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.entity.*

typealias FriendList = UserList<out Friend>
typealias GroupList = UserList<out Group>
typealias MemberList = UserList<out Member>
typealias GuildList = UserList<out Guild>
typealias GuildMemberList = UserList<out GuildMember>