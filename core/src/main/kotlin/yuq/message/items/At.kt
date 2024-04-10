package yuq.message.items

import yuq.message.MessageItem

class At(var target: String) : MessageItem {
    override val logString: String
        get() = "At_$target"
}