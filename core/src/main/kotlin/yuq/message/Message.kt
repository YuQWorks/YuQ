package yuq.message

import yuq.annotation.Internal
import yuq.annotation.NoRecommendation
import yuq.message.chain.MessageBody
import yuq.message.items.Text

class Message(val body: MessageBody) : MessageItemChain by body, MessagePlusAble {

    /*** 消息源信息。
     * 与消息本身无关，如果这条消息是收到的消息，则会附带本参数。
     * 消息源是定位消息在腾讯所在位置的记录，用于消息撤回，回复等操作。
     * 当你将消息发出时，并不会将发出消息的消息源写到本参数，而是 sendMessage 方法返回的消息源。
     */
    var source: MessageSource? = null

    /*** 回复消息
     * 描述一条消息是否是对另一条消息的回复。
     * 如果收到的消息有回复目标，则会附带本参数。
     * 如果你需要发出一条回复消息，则需要将本参数设置为你要回复的消息的消息源。
     */
    var reply: MessageSource? = null

    /*** At 参数
     * 使得消息发送时可以自动 At 目标。
     * 本参数是一个纯粹的发送参数，接收到的消息本参数永远为空。
     */
    var at: MessageAt? = null

    /*** 消息撤回参数
     * 本条消息在发出后一段时间撤回，单位：毫秒。
     * 本参数是一个纯粹的发送参数，接收到的消息本参数永远为空。
     */
    var recallDelay: Long? = null

    /*** 撤回消息
     * 该方法是 source 字段的 recall 方法快捷调用。
     * 当 source 为 null 时会直接报错，请注意。
     * @see MessageSource.recall
     */
    suspend fun recall() {
        return source!!.recall()
    }


    fun bodyEquals(other: Any?): Boolean {
        if (other !is com.icecreamqaq.yuq.message.Message) return false
        if (body.size != other.body.size) return false
        for ((i, item) in body.withIndex()) {
            val oi = other.body[i]
            if (item != oi) return false
        }
        return true
    }

    override fun toString(): String {
        return toLogString()
    }

    @Internal
    @NoRecommendation
    fun toLogString(): String {
        val sb = StringBuilder("(")
        if (reply != null) sb.append("Reply To: ${reply!!.id}, ")
        if (at != null) sb.append("At them${if (at!!.newLine) " \\n" else ""}, ")
        if (body.size > 0) {
            sb.append("[ ${body[0].logString}")
            for (i in 1 until body.size) {
                sb.append(", ${body[i].logString}")
            }
            sb.append(" ]")
        }
        sb.append(")")
        return sb.toString()
    }


    constructor(item: MessageItem) : this(MessageBody().apply { append(item) })

    override fun plus(item: MessageItem): Message = apply { body.append(item) }

    override fun plus(item: String): Message = apply { body.append(Text(item)) }

    override fun plus(item: Message): Message = apply { body.append(item.body) }

    override fun plus(item: MessageBody): Message = apply { body.append(item) }

}