package test.yuq.event

import com.IceCreamQAQ.Yu.event.events.Event
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group

class RegisterContactEvent(val friends: MutableMap<Long, Friend>, val groups: MutableMap<Long, Group>) : Event()