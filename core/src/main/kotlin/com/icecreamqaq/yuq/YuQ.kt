package com.icecreamqaq.yuq

import com.icecreamqaq.yuq.message.MessageItemFactory

interface YuQ {

    val messageItemFactory: MessageItemFactory

    val bots: List<Bot>

    fun findByPlatformAndPlatformId(platform: String, platformId: String): Bot? =
        bots.firstOrNull { it.platform == platform && it.botInfo.platformId == platformId }

    /*** 动态创建 Bot 实例。
     * @param id 登录账户 ID。
     * @param pwd 登录账户密码。
     * @param botName 机器人名
     * @param extData 目标 Runtime 所需要的扩展数据。
     * @return 创建后的机器人对象。
     *
     * 根据目标平台不同，创建 Bot 具体所需要的参数与结果并不相同。
     */
    fun createBot(id: String, pwd: String, botName: String? = null, extData: String? = null): Bot

}