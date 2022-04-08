package com.icecreamqaq.yuq.util

import com.icecreamqaq.yuq.message.*

val Message.liteMessage: String get(){
    val sb = StringBuilder()

    if (this.at != null) sb.append("@${this.at!!.id} ")
    for (item in body) {
        sb.append(
            when (item) {
                is Text -> item.text
                is At -> "@${item.user} "
                is Face -> "[表情]"
                is Image -> "[图片]"
                else -> "暂不支持"
            }
        )
    }
    return sb.toString()
}