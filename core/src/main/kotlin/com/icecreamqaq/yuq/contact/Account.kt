package com.icecreamqaq.yuq.contact

/*** 账号
 * 用于描述一个标准用户，可能由添加好友、群成员等渠道获得。
 *
 * 账号可能存在与之关联的联系人，也可能不存在。
 */
interface Account {

    // 账号 ID
    val id: Long
    // 账号 Platform 实际用户 ID
    val platformId: String

    // 账号昵称
    val nickname: String
    // 账号头像 URL 链接
    val avatar: String
}