package com.icecreamqaq.yuq.message

import com.icecreamqaq.yuq.annotation.NoRecommendation
import com.icecreamqaq.yuq.entity.MessageAt
import com.icecreamqaq.yuq.error.MessageThrowable
import com.icecreamqaq.yuq.message.Image.Companion.toFlash
import com.icecreamqaq.yuq.message.Text.Companion.toText
import com.icecreamqaq.yuq.mif
import java.io.File

interface MessagePlus {
    operator fun plus(item: MessageItem): MessageItemChain
    operator fun plus(item: String): MessageItemChain
    operator fun plus(item: Message): MessageItemChain
    operator fun plus(item: MessageItemChain): MessageItemChain
}

interface SendAble {
    fun toMessage(): Message
    fun toThrowable(): MessageThrowable = MessageThrowable(toMessage())
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

data class MessageFailByCancel(
    override val id: Int,
    override val sender: Long,
    override val sendTime: Long,
    override val liteMsg: String,
    override val groupCode: Long,
) : GroupMessageSource, TempMessageSource {
    override fun recall(): Int {
        return 0
    }
}

data class MessageFailByReadTimeOut(
    override val id: Int,
    override val sender: Long,
    override val sendTime: Long,
    override val liteMsg: String,
    override val groupCode: Long,
) : GroupMessageSource, TempMessageSource {
    override fun recall(): Int {
        return 0
    }
}


open class Message(val body: MessageItemChain = MessageItemChain()) : SendAble, IMessageItemChain by body {

    var id: Int? = null

    /***
     * 消息源信息。
     * 与消息本身无关，如果这条消息是收到的消息，则会附带本参数。
     * 消息源是定位消息在腾讯所在位置的记录，用于消息撤回，回复等操作。
     * 当你将消息发出时，并不会将发出消息的消息源写到本参数，而是 sendMessage 方法返回的消息源。
     */
    lateinit var source: MessageSource

    var codeStr: String = ""
        get() {
            if (field == "") field = toCodeString()
            return field
        }
    var reply: MessageSource? = null
    var at: MessageAt? = null

    @NoRecommendation
    lateinit var sourceMessage: Any

    lateinit var path: List<MessageItem>

    private var lineQ_: MessageLineQ? = null
    fun lineQ(): MessageLineQ {
        if (lineQ_ == null) lineQ_ = MessageLineQ(this)
        return lineQ_!!
    }

    /***
     * 本条消息在发出后一段时间撤回，单位：毫秒。
     */
    var recallDelay: Long? = null

    fun toLogString(): String {
        val sb = StringBuilder("(")
        if (reply != null) sb.append("Reply To: ${reply!!.id}, ")
        if (at != null) sb.append("At them${if (at!!.newLine) " \\n" else ""}, ")
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

    override fun toMessage() = this

    operator fun plus(item: MessageItem): Message {
        body.append(item)
        return this
    }

    operator fun plus(item: String): Message {
        body.append(item.toText())
        return this
    }

    operator fun plus(item: Message): Message {
        body.append(item.body)
        return this
    }

    operator fun plus(item: MessageItemChain): Message {
        body.append(item)
        return this
    }

    companion object {

        fun Message.firstString(): String {
            for (item in body) {
                if (item is Text) return item.text
            }
            error("消息不包含任何一个文本串。")
        }

        fun Message.toCodeString(): String {
            val sb = StringBuilder()
            if (reply != null) sb.append("<Rain:Reply:${reply!!.id}>")

            for (item in body) {
                sb.append(
                    when (item) {
                        is Text -> item.text
                        is At -> "<Rain:At:${item.user}>"
                        is Face -> "<Rain:Face:${item.faceId}>"
                        is Image -> "<Rain:Image:${item.id}${if (item is FlashImage) ", Flash>" else ">"}"
                        is XmlEx -> "<Rain:Xml:${item.serviceId},${
                            item.value.replace("<", "&&&lt&&&").replace(">", "&&&gt&&&")
                        }>"
                        is JsonEx -> "<Rain:Json:${item.value}>"
                        is Voice -> "<Rain:Voice:${item.id}>"
                        else -> "<Rain:NoImpl:${item.toPath()}>"
                    }
                )
            }
            return sb.toString()
        }

        fun String.toMessageByRainCode(): Message {
            val codeStart = "<Rain:"

            var message = MessageItemChain()
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
                                    else if (id.startsWith("http", true) || url) mif.imageByUrl(id)
                                    else mif.imageById(id)
                                    if (flash) p.toFlash()
                                    else p
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

            return message.toMessage()
        }

        fun String.toMessage() = this.toText().toMessage()

        fun String.toChain() = MessageItemChain().append(this.toText())

    }
}