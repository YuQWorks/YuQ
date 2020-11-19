package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.RainCode
import com.icecreamqaq.yuq.entity.MessageAt
import com.icecreamqaq.yuq.error.MessageThrowable
import com.icecreamqaq.yuq.message.Image.Companion.toFlash
import com.icecreamqaq.yuq.message.Text.Companion.toText
import com.icecreamqaq.yuq.mif
import java.io.File

interface MessagePlus {
    operator fun plus(item: MessageItem): Message
    operator fun plus(item: String): Message
    operator fun plus(item: Message): Message
}

interface MessageSource {
    val id: Int
    val sender: Long
    val sendTime: Long
    val liteMsg: String

    fun recall(): Int
}

interface GroupMessageSource : MessageSource {
    val groupCode: Long
}

interface TempMessageSource : MessageSource {
    val groupCode: Long
}

open class Message : /*Result(),*/ MessagePlus {

//    @Deprecated("相关 API 已经调整，现在建议直接使用 Contact 对象发送消息。")
//    var temp: Boolean = false

    var id: Int? = null

//    @Deprecated("相关 API 已经调整，现在建议直接使用 Contact 对象发送消息。")
//    var qq: Long? = null
//
//    @Deprecated("相关 API 已经调整，现在建议直接使用 Contact 对象发送消息。")
//    var group: Long? = null

    lateinit var source: MessageSource
    var codeStr: String = ""
        get() {
            if (field == "") field = toCodeString()
            return field
        }
    var reply: MessageSource? = null
    var at: MessageAt? = null

    lateinit var sourceMessage: Any
    var body = ArrayList<MessageItem>()
    lateinit var path: List<MessageItem>

    fun toLogString(): String {
        val sb = StringBuilder("(")
        if (reply != null) sb.append("Reply To: ${reply!!.id}, ")
        if (at != null) sb.append("At them${if (at!!.newLine) " \n" else ""}, ")
        if (body.size > 0) {
            sb.append("[ ${body[0].toLogString()}")
            for (i in 1 until body.size) {
                sb.append(", ${body[i].toLogString()}")
            }
            sb.append(" ]")
        }
        sb.append(")")
        return sb.toString()
    }

    fun toPath(): List<String> {
        val paths = ArrayList<String>()
        for (item in path) {
            paths.add(item.toPath())
        }
        return paths
    }

    override operator fun plus(item: MessageItem): Message {
        body.add(item)
        return this
    }

    override fun plus(item: String): Message {
        body.add(item.toText())
        return this
    }

    override fun plus(item: Message): Message {
        body.addAll(item.body)
        return this
    }

//    @Deprecated("相关 API 已经调整，现在建议直接使用 Contact 对象发送消息。")
//    fun newMessage(): Message {
//        val message = Message()
//
//        message.qq = this.qq
//        message.group = this.group
//        message.temp = this.temp
//        return message
//    }

    fun recall(): Int {
        return source.recall()
    }

    override fun toString(): String {
        return toLogString()
    }


    open fun bodyEquals(other: Any?): Boolean {
        if (other !is Message) return false
        if (body.size != other.body.size) return false
        for ((i, item) in body.withIndex()) {
            val oi = other.body[i]
            if (item != oi) return false
        }
        return true
    }

    fun toThrowable() = MessageThrowable(this)

    companion object {

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
                            is XmlEx -> "<Rain:Xml:${item.serviceId},${item.value.replace("<", "&&&lt&&&").replace(">", "&&&gt&&&")}>"
                            is JsonEx -> "<Rain:Json:${item.value}>"
                            else -> "<Rain:NoImpl:${item.toPath()}>"
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
                            val code = codeStr.split(":", limit = 3)
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
                                    val ps = data.split(",")
                                    var url = false
                                    var file = false
                                    var flash = false
                                    val id = ps[0]
                                    for (i in 1 until ps.size) {
                                        val p = ps[i]
                                        if (p == "url") url = true
                                        if (p == "file") file = true
                                        if (p == "flash") flash = true
                                    }
                                    val p = if (file) mif.imageByFile(File(id))
                                    else if ((RainCode.matchImageIdStartHttp && id.startsWith("http", true)) || url) mif.imageByUrl(id)
                                    else mif.imageById(id)
                                    if (flash) p.toFlash()
                                    else p
//                                    if (id.st)
//                                    val p = data.indexOf(',')
//                                    if (p == -1) mif.imageById(data)
//                                    else {
//                                        val id = data.substring(0, p)
//                                        mif.imageById(id).toFlash()
//                                    }
                                }
                                "Xml" -> {
                                    val a = data.split(",", ignoreCase = false, limit = 2)
                                    val id = a[0].toInt()
                                    val value = a[1].replace("&&&lt&&&", "<").replace("&&&gt&&&", ">")
                                    mif.xmlEx(id, value)
                                }
                                "Json" -> mif.jsonEx(data)
                                else -> mif.text("$codeStr>")
                            }

                            m.clear()
                            rf = false
                            rc = false
                        }
                    } else {
                        if (c == '<') {
                            t.append(m.toString())
                            m.clear()
                            m.append('<')
                            continue
                        }
                        m.append(c)
                        if (m.length >= 6) {
                            val ms = m.toString()
                            if (codeStart == ms) rc = true
                            else {
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

//        fun String.toMessageByRainCode(): Message {
//            val codeStart = "<Rain:"
//
//            var message = Message()
//            val t = StringBuilder()
//            val m = StringBuilder()
//
//            var rf = false
//            var rc = false
//
//            for (c in this) {
//                if (rf) {
//                    if (rc) {
//                        if (c != '>') m.append(c)
//                        else {
//                            if (t.isNotEmpty()) {
//                                message += mif.text(t.toString())
//                                t.clear()
//                            }
//                            val codeStr = m.toString()
//                            val code = codeStr.split(":")
//                            if (code.size < 3) {
//                                t.append(codeStr).append(">")
//                                m.clear()
//                                rf = false
//                                rc = false
//                                continue
//                            }
//                            val type = code[1]
//                            val data = code[2]
//                            message += when (type) {
//                                "At" -> mif.at(data.toLong())
//                                "Face" -> mif.face(data.toInt())
//                                "Image" -> {
//                                    val ps = data.split(",")
//                                    var url = false
//                                    var file = false
//                                    var flash = false
//                                    val id = ps[0]
//                                    for (i in 1 until ps.size) {
//                                        val p = ps[i]
//                                        if (p == "url") url = true
//                                        if (p == "file") file = true
//                                        if (p == "flash") flash = true
//                                    }
//                                    val p = if (file) mif.imageByFile(File(id))
//                                    else if ((RainCode.matchImageIdStartHttp && id.startsWith("http", true)) || url) mif.imageByUrl(id)
//                                    else mif.imageById(id)
//                                    if (flash) p.toFlash()
//                                    else p
////                                    if (id.st)
////                                    val p = data.indexOf(',')
////                                    if (p == -1) mif.imageById(data)
////                                    else {
////                                        val id = data.substring(0, p)
////                                        mif.imageById(id).toFlash()
////                                    }
//                                }
//                                "Xml" -> {
//                                    val a = data.split(",", ignoreCase = false, limit = 2)
//                                    val id = a[0].toInt()
//                                    val value = a[1].replace("&&&lt&&&", "<").replace("&&&gt&&&", ">")
//                                    mif.xmlEx(id, value)
//                                }
//                                "Json" -> mif.jsonEx(data)
//                                else -> mif.text("$codeStr>")
//                            }
//
//                            m.clear()
//                            rf = false
//                            rc = false
//                        }
//                    } else {
//                        if (m.length < 6) m.append(c)
//                        else {
//                            val ms = m.toString()
//                            if (codeStart == ms) {
//                                rc = true
//                                m.append(c)
//                            } else {
//                                t.append(ms)
//                                m.clear()
//                                rf = false
//                                rc = false
//                                continue
//                            }
//                        }
//                    }
//                } else {
//                    if (c != '<') t.append(c)
//                    else {
//                        m.append(c)
//                        rf = true
//                    }
//                }
//
//            }
//
//            if (m.isNotEmpty()) t.append(m)
//            if (t.isNotEmpty()) message += mif.text(t.toString())
//
//            return message
//        }

        fun String.toMessage() = this.toText().toMessage()

    }
}