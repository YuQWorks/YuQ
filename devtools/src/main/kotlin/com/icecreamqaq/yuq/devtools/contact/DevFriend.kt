package com.icecreamqaq.yuq.devtools.contact

import com.icecreamqaq.yuq.contact.Friend
import com.icecreamqaq.yuq.devtools.DevBot
import com.icecreamqaq.yuq.event.FriendDeleteEvent
import com.icecreamqaq.yuq.post

class DevFriend(bot: DevBot, id: Long, nickname: String) :Friend, DevContact(bot, id, nickname) {

    override val guid: String = "${bot.botId}_f_$id"
    override val logString: String = "$nickname($id)"

    override fun delete() {
        bot.friends.remove(this)
        FriendDeleteEvent(this).post()
    }

}