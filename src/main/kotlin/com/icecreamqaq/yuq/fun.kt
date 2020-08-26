package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.*

lateinit var yuq: YuQ

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

fun Message.toCodeString(): String {
    val sb = StringBuilder()
    if (reply != null) sb.append("<Rain:Reply:$id>")

    for (item in body) {
        sb.append(
                when (item) {
                    is Text -> item.text
                    is At -> "<Rain:At:${item.user}>"
                    is Face -> "<Rain:Face:${item.faceId}>"
                    is Image -> "<Rain:Image:${item.id}${if (item is FlashImage) ", Flash>" else ">"}"
                    else -> "<Rain:NoImpl:${item.toPath()}"
                }
        )
    }
    return sb.toString()
}

fun String.toMessageByRainCode(): Message {
    val codeStart = "<Rain:"

    var message = Message()
    val t = StringBuilder()
    val m = StringBuilder()

    var rf = false
    var rc = false

    for (c in this) {
        if (rf) {
            if (rc) {
                if (c != '>') m.append(c)
                else {
                    if (t.isNotEmpty()) {
                        message += mif.text(t.toString())
                        t.clear()
                    }
                    val codeStr = m.toString()
                    val code = codeStr.split(":")
                    if (code.size < 3) {
                        t.append(codeStr).append(">")
                        m.clear()
                        rf = false
                        rc = false
                        continue
                    }
                    val type = code[1]
                    val data = code[2]
                    message += when (type) {
                        "At" -> mif.at(data.toLong())
                        "Face" -> mif.face(data.toInt())
                        "Image" -> {
                            val p = data.indexOf(',')
                            if (p == -1) mif.imageById(data)
                            else {
                                val id = data.substring(0,p)
                                mif.imageById(id).toFlash()
                            }
                        }
                        else -> mif.text("$codeStr>")
                    }

                    m.clear()
                    rf = false
                    rc = false
                }
            } else {
                if (m.length < 6) m.append(c)
                else {
                    val ms = m.toString()
                    if (codeStart == ms) {
                        rc = true
                        m.append(c)
                    } else {
                        t.append(ms)
                        m.clear()
                        rf = false
                        rc = false
                        continue
                    }
                }
            }
        } else {
            if (c != '<') t.append(c)
            else {
                m.append(c)
                rf = true
            }
        }

    }

    if (m.isNotEmpty()) t.append(m)
    if (t.isNotEmpty()) message += mif.text(t.toString())

    return message
}

fun Image.toFlash() = mif.imageToFlash(this)

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