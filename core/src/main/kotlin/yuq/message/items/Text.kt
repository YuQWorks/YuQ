package yuq.message.items

import yuq.message.MessageItem

class Text(val text: String) : MessageItem {
    override val logString: String
        get() = text
}