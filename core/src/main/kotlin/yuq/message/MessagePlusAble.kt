package yuq.message

import yuq.message.chain.MessageBody

interface MessagePlusAble {
    operator fun plus(item: MessageItem): MessageItemChain
    operator fun plus(item: String): MessageItemChain
    operator fun plus(item: Message): MessageItemChain
    operator fun plus(item: MessageBody): MessageItemChain
}