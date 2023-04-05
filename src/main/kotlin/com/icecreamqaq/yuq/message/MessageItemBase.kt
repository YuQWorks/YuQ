package com.icecreamqaq.yuq.message

abstract class MessageItemBase : MessageItem, SendAble {

    override operator fun plus(item: MessageItem): MessageItemChain = toItemChain() + item
    override operator fun plus(item: String): MessageItemChain = toItemChain() + item
    override operator fun plus(item: Message): MessageItemChain = toItemChain() + item
    override fun plus(item: MessageItemChain): MessageItemChain = item.unshift(this)
    override fun toMessage(): Message = this.toItemChain().toMessage()

    fun toItemChain() = MessageItemChain().append(this)

    abstract fun equal(other: MessageItem): Boolean

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is MessageItem) return false
        return equal(other)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString() = logString
}