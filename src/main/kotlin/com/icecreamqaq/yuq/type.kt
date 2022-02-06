package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.entity.UserList

typealias FriendList = UserList<out Friend>
typealias GroupList = UserList<out Group>
typealias MemberList = UserList<out Member>