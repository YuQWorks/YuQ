package test.yuq.message

import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.message.At
import com.icecreamqaq.yuq.message.Face
import com.icecreamqaq.yuq.message.FlashImage
import com.icecreamqaq.yuq.message.Image
import com.icecreamqaq.yuq.message.MessagePackage
import com.icecreamqaq.yuq.message.Voice
import kotlinx.coroutines.runBlocking
import java.io.InputStream

abstract class MessageItemBaseX:MessageItemBase(){
    override fun toLocal(contact: Contact) = ""
}

class TextImpl(override var text: String) : MessageItemBaseX(), Text {

}

class AtImpl(override var user: Long) : MessageItemBaseX(), At {

}

class AtMemberImpl(override val member: Member) : MessageItemBaseX(), AtByMember {
}

class FaceImpl(override val faceId: Int) : MessageItemBaseX(), Face {

}

open class ImageReceive(id: String, override val url: String) : MessageItemBaseX(), Image {

    override val id: String = if (id.startsWith("{")) id.replace("{", "").replace("}", "").replace("-", "") else id


}


class FlashImageImpl(override val image: Image) : MessageItemBaseX(), FlashImage {
    override fun toPath() = "闪照"
}

class XmlImpl(override val serviceId: Int, override val value: String) : MessageItemBaseX(), XmlEx {

}

class JsonImpl(override val value: String) : MessageItemBaseX(), JsonEx {

}

class NoImplItemImpl(override var source: Any) : MessageItemBaseX(), NoImplItem {
}

class MessagePackageImpl(override var type: Int, override val body: MutableList<IMessageItemChain>):MessageItemBaseX(),MessagePackage{
    override fun toString(): String {
        return "MessagePackage(type=$type, body=$body)"
    }
}

