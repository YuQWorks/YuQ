package yuq.message.source

import yuq.message.MessageSource

class MessageFailByCancel : MessageSource {
    override val id: String
        get() = "MessageFailByCancel"

    override suspend fun recall() {

    }
}