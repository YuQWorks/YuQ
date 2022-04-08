package test.yuq.message

import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.message.Text
import kotlin.random.Random

class MessageSourceImpl(override val sender: Long, override val sendTo: Long) : MessageSource {
    override val id: Int = Random.nextInt()
    override val liteMsg: String = ""
    override val sendTime: Long = System.currentTimeMillis()

    override fun recall(): Int {
        println("撤回来自 $sender 在 $sendTime 发送的消息 $id。")
        return 0
    }
}

fun Message.make(): Message {
    val path = arrayListOf<MessageItem>()

    body.forEach {
        if (it is Text){
            val sm = it.text
            if (sm.isNotEmpty()) {
                val sms = sm.replace("\n", " ").split(" ")
                var loopStart = 0
//                if (itemNum == 0 && botName != null && sms[0] == botName) loopStart = 1
                for (i in loopStart until sms.size) {
                    path.add(TextImpl(sms[i]))
//                    itemNum++
                }
            }

        }else path.add(it)
    }

    this.path = path
    this.sourceMessage = this
    return this
}