package yuq.message.items

import yuq.message.MessageItem

class Image(
    var id: String,
    var platform: String,
    var url: String
) : MessageItem {
    override val logString: String
        get() = "img_$id"
}