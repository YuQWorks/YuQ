package com.icecreamqaq.yuq.message

import com.IceCreamQAQ.Yu.entity.Result

interface MessagePlus {
    operator fun plus(item: MessageItem): Message
    operator fun plus(item: String): Message
    operator fun plus(item: Message): Message
}

interface MessageSource {
    val id: Int

    fun recall(): Int
}

abstract class Message : Result(), MessagePlus {

    var temp: Boolean = false

    var id: Int? = null
    var qq: Long? = null
    var group: Long? = null

    lateinit var source: MessageSource
    var reply: MessageSource? = null
    var at = false

    lateinit var sourceMessage: Any
    var body = ArrayList<MessageItem>()
    lateinit var path: List<MessageItem>

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

    override fun plus(item: Message): Message {
        body.addAll(item.body)
        return this
    }

    abstract fun newMessage(): Message

    fun recall(): Int {
        return source.recall()
    }

    override fun toString(): String {
        val sb = StringBuilder("Sender: $qq, ")
        if (group != null) sb.append("Group: $group, ")
        if (temp) sb.append("Temp Message, ")
        if (reply != null) sb.append("Reply To: ${reply!!.id}, ")
        if (at) sb.append("At them, ")
        if (body.size > 0) {
            sb.append("Body: [ ${body[0]}")
            for (i in 1 until body.size) {
                sb.append(", ${body[i]}")
            }
            sb.append(" ]")
        }
        return sb.toString()
    }

    open fun bodyEquals(other: Any?): Boolean {
        if (other !is Message) return false
        if (body.size != other.body.size) return false
        for ((i,item) in body.withIndex()) {
            val oi = other.body[i]
            if (item != oi)return false
        }
        return true
    }
}