package yuq.message.source

import yuq.message.MessageSource

class MessageFailByReadTimeOut: MessageSource {
    override val id: String
        get() = "MessageFailByReadTimeOut"

    override suspend fun recall() {

    }
}