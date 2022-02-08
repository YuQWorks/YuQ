package com.icecreamqaq.yuq.entity

import com.icecreamqaq.yuq.GuildMemberList

interface Guild:User {

//    val id: Long
//    val platformId: Long
//
//    val name: String
//    val avatar: String

    val defaultChannel: Channel
    val channels: List<Channel>

    // 该列表并不提供完整的成员列表！
    val member: GuildMemberList

}

interface Channel : Contact {
    val guild: Guild
    override val avatar: String
        get() = ""
}

interface GuildMember : Contact