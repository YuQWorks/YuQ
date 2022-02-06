package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Group
import com.icecreamqaq.yuq.entity.User
import com.icecreamqaq.yuq.entity.UserList
import com.icecreamqaq.yuq.message.MessageItemFactory

@AutoBind
interface YuQ {

    val messageItemFactory: MessageItemFactory

    /***
     * 机器人的 QQ 号码
     */
    val botId: Long

    /***
     * 机器人的个人信息
     */
    val botInfo: User

    /***
     * 好友列表
     */
    val friends: FriendList

    /***
     * 群列表
     */
    val groups: GroupList

    /***
     * 刷新好友列表
     */
    fun refreshFriends(): FriendList

    /***
     * 刷新群列表
     */
    fun refreshGroups(): GroupList

//    /***
//     * 发送消息。
//     */
//    @Deprecated("建议直接使用 Contact 对象的 sendMessage 方法。")
//    fun sendMessage(message: Message): MessageSource
//
//    /***
//     * 撤回消息。
//     */
//    @Deprecated("建议直接使用 Message 或 MessageSource 的 recall 方法。")
//    fun recallMessage(messageSource: MessageSource): Int

    val cookieEx: QQCookie
    val web: Web

    interface QQCookie {
        val skey: String
        val gtk: Long
        val pskeyMap: Map<String, Pskey>

        data class Pskey(val pskey: String, val gtk: Long)
    }

    companion object {

        fun getYuQ(packageName: String): YuQ {
            return yuq
        }

    }

}