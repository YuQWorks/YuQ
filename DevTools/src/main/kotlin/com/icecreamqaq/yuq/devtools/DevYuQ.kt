package com.icecreamqaq.yuq.devtools

import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.YuQ
import com.icecreamqaq.yuq.devtools.contact.DevAccount
import com.icecreamqaq.yuq.message.MessageItemFactory

class DevYuQ(
    override val messageItemFactory: MessageItemFactory
) : YuQ {
    override val bots: List<Bot> = ArrayList()

    override fun createBot(id: String, pwd: String, botName: String?, extData: String?): Bot {
//        return DevBot(DevAccount(id,""), arrayListOf())
        TODO()
    }

}