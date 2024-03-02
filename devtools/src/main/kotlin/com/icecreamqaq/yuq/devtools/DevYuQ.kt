package com.icecreamqaq.yuq.devtools

import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.devtools.contact.*
import com.icecreamqaq.yuq.event.YuQApplicationStatusChanged
import com.icecreamqaq.yuq.message.MessageItemFactory
import rain.api.loader.ApplicationService

class DevYuQ(
    override val messageItemFactory: MessageItemFactory,
    val service: BotService,
    configBots: List<ConfigBot>
) : YuQ, ApplicationService {

    data class ConfigMember(
        var id: Long = 0,
        var name: String = "",
        var permission: Int = 0,
        var title: String = "",
        var namecard: String = "",
        var ban: Long = 0,
        var lastMessageTime: Long = 0
    )

    data class ConfigGroup(
        var id: Long = 0,
        var name: String = "",
        var maxCount: Int = 500,
        var members: List<ConfigMember> = ArrayList()
    )

    data class ConfigFriend(
        var id: Long = 0,
        var name: String = ""
    )

    data class ConfigBot(
        var id: Long = 0,
        var name: String = "",
        var friends: List<ConfigFriend> = ArrayList(),
        var groups: List<ConfigGroup> = ArrayList()
    )

    override var bots = ArrayList<DevBot>()

    init {
        configBots.forEach {
            DevBot(DevAccount(it.id, it.name)).apply {
                bots.add(this)
                it.friends.forEach { f ->
                    DevFriend(this, f.id, f.name).apply {
                        friends.add(this)
                    }
                }
                it.groups.forEach { cg ->
                    DevGroup(this, cg.id, cg.name, cg.maxCount).also { group ->
                        groups.add(group)
                        group.initMembers(
                            cg.members.map { cm ->
                                DevGroupMember(
                                    this,
                                    group,
                                    cm.id,
                                    cm.name,
                                    cm.namecard,
                                    cm.permission,
                                    cm.title,
                                    cm.ban,
                                    cm.lastMessageTime
                                )
                            }
                        )
                    }
                }
                login()
            }
        }
        println("")
    }


    override fun createBot(id: String, pwd: String, botName: String?, extData: String?): Bot {
        return DevBot(DevAccount(id.toLong(), "")).apply { bots.add(this) }
    }


    override fun start() {
    }

    override fun stop() {
    }


}