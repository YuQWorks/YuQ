package test.yuq.event

import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.FriendList
import com.icecreamqaq.yuq.GroupList
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group

class RegisterContactEvent(val friends: FriendList, val groups: GroupList) : Event()