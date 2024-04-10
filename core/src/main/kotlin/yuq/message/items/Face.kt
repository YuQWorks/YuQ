package yuq.message.items

import yuq.message.MessageItem

class Face(val id: Int): MessageItem {
    override val logString: String
        get() = "face_$id"
}