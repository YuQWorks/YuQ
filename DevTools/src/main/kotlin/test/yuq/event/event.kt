package test.yuq.event

import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.FriendList
import com.icecreamqaq.yuq.GroupList

class RegisterContactEvent(val friends: FriendList, val groups: GroupList) : Event()