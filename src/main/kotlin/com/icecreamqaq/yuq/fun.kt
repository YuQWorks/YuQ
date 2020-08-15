package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.*
import java.lang.StringBuilder

lateinit var yuq:YuQ
@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
lateinit var mf: MessageFactory
lateinit var mif: MessageItemFactory
lateinit var web: Web

//operator fun String.minus(messageItem: MessageItem) = mif.text(this) + messageItem
fun String.toText() = mif.text(this)
fun String.toMessage() = this.toText().toMessage()

@Deprecated("相关 API 变动，Message 已经不再承载消息目标，请直接 new Message。")
fun Message.send() = yuq.sendMessage(this)
fun Message.firstString(): String {
    for (item in body) {
        if (item is Text) return item.text
    }
    error("消息不包含任何一个文本串。")
}

fun Message.toCodeString() :String{
    val sb = StringBuilder()
    if (reply != null)sb.append("<Rain:Reply:$id>")

    for (item in body) {
        when(item){
            is Text -> sb.append(item.text)
            is At -> sb.append("<Rain:At:${item.user}>")
            is Face -> sb.append("<Rain:Face:${item.faceId}>")
            is Image -> sb.append("<Rain:Image:${item.id}${if (item is FlashImage) ", Flash>" else ">"}")
        }
    }
    return sb.toString()
}
fun Member.toFriend(): Friend? = yuq.friends[id]

fun Web.getWithQQKey(url: String) = this.get(convertUrl(url))
fun Web.postWithQQKey(url: String, para: MutableMap<String, String>): String {
    for ((k, v) in para) {
        para[k] = when (v) {
            "{gtk}" -> yuq.cookieEx.gtk.toString()
            "{skey}" -> yuq.cookieEx.skey
            "{psgtk}" -> {
                val domain = url.split("://")[1].split("/")[0]
                var psgtk = ""
                for ((k, v) in yuq.cookieEx.pskeyMap) {
                    if (domain.endsWith(k)) {
                        psgtk = v.gtk.toString()
                        break
                    }
                }
                psgtk
            }
            else -> v
        }
    }
    return this.post(convertUrl(url), para)
}

fun convertUrl(url: String): String {
    var u = url.replace("{gtk}", yuq.cookieEx.gtk.toString(), true)
    u = u.replace("{skey}", yuq.cookieEx.skey, true)
    if (u.contains("{psgtk}", true)) {
        val domain = u.split("://")[1].split("/")[0]
        for ((k, v) in yuq.cookieEx.pskeyMap) {
            if (domain.endsWith(k)) {
                u = u.replace("{psgtk}", v.gtk.toString(), true)
                break
            }
        }
    }
    return u
}