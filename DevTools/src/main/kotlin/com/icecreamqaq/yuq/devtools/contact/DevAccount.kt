package com.icecreamqaq.yuq.devtools.contact

import com.icecreamqaq.yuq.Bot
import com.icecreamqaq.yuq.contact.Account

open class DevAccount(
    override val id: Long,
    override val nickname: String
) : Account {
    override val platformId: String
        get() = id.toString()
    override val avatar: String
        get() = "https://q1.qlogo.cn/g?b=qq&nk=$id&s=640"
}