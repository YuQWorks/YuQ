package yuq.message

import yuq.message.chain.MessageBody
import yuq.message.items.Text

interface MessageItem : MessagePlusAble, SendAble {
    val logString: String

    companion object {
        private fun MessageItem.messageOf(body: Message.() -> Unit) = toMessage().apply(body)
    }

    override fun toMessage(): Message =
        Message(this)

    override fun plus(item: Message): Message =
        messageOf { body.append(item.body) }

    override fun plus(item: MessageBody): Message =
        messageOf { body.append(item) }

    override fun plus(item: MessageItem): Message =
        messageOf { body.append(item) }

    override fun plus(item: String): Message =
        messageOf { body.append(Text(item)) }
}