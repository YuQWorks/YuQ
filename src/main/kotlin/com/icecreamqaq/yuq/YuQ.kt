package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageFactory
import com.icecreamqaq.yuq.message.MessageItemFactory
import com.icecreamqaq.yuq.message.MessageSource

@AutoBind
interface YuQ {

    @Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
    val messageFactory: MessageFactory
    val messageItemFactory: MessageItemFactory

    /***
     * 机器人的 QQ 号码
     */
    val botId: Long

    /***
     * 好友列表
     */
    val friends: Map<Long, Friend>

    /***
     * 群列表
     */
    val groups: Map<Long, Group>

    /***
     * 刷新好友列表
     */
    fun refreshFriends(): Map<Long, Friend>

    /***
     * 刷新群列表
     */
    fun refreshGroups(): Map<Long, Group>

    /***
     * 发送消息。
     */
    @Deprecated("建议直接使用 Contact 对象的 sendMessage 方法。")
    fun sendMessage(message: Message): MessageSource

    /***
     * 撤回消息。
     */
    @Deprecated("建议直接使用 Message 或 MessageSource 的 recall 方法。")
    fun recallMessage(messageSource: MessageSource): Int

}