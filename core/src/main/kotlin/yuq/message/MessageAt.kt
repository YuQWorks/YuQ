package yuq.message

// 用以描述在消息发送时自动 At 被接收方。
data class MessageAt(
    // At 后是否插入一个换行
    val newLine: Boolean = false
)