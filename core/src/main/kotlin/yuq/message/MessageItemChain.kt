package yuq.message


interface MessageItemChain {

    companion object {
        inline fun <reified T : MessageItem> MessageItemChain.firstBy() = first(T::class.java)
        inline fun <reified T : MessageItem> MessageItemChain.lastBy() = last(T::class.java)
        inline fun <reified T : MessageItem> MessageItemChain.findBy() = find(T::class.java)
    }

    operator fun get(index: Int): MessageItem
    fun unshift(item: MessageItem): MessageItemChain
    fun append(item: MessageItem): MessageItemChain

    fun <T : MessageItem> first(type: Class<T>): T?
    fun <T : MessageItem> last(type: Class<T>): T?
    fun <T : MessageItem> find(type: Class<T>): List<T>

    fun first(): MessageItem?
    fun last(): MessageItem?
}