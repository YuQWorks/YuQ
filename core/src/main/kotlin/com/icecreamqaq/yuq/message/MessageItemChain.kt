package com.icecreamqaq.yuq.message


data class ChainItem(var item: MessageItem, var next: ChainItem? = null, var previous: ChainItem? = null)

interface IMessageItemChain {

    operator fun get(index: Int): MessageItem
    fun unshift(item: MessageItem): MessageItemChain
    fun append(item: MessageItem): MessageItemChain
    fun append(chain: MessageItemChain): MessageItemChain

    fun <T : MessageItem> first(type: Class<T>): T?
    fun <T : MessageItem> last(type: Class<T>): T?
    fun <T : MessageItem> find(type: Class<T>): List<T>

    fun first(): MessageItem?
    fun last(): MessageItem?

}

inline fun <reified T : MessageItem> IMessageItemChain.firstBy() = first(T::class.java)
inline fun <reified T : MessageItem> IMessageItemChain.lastBy() = last(T::class.java)
inline fun <reified T : MessageItem> IMessageItemChain.findBy() = find(T::class.java)

class MessageItemChain : IMessageItemChain, List<MessageItem>, MessagePlus, SendAble {

    private var _size = 0
    override val size get() = _size
    private var first: ChainItem? = null
    private var last: ChainItem? = null

    override fun first() = first?.item
    override fun last() = last?.item

    override fun <T : MessageItem> first(type: Class<T>): T? = firstOrNull { type.isInstance(it) } as? T
    override fun <T : MessageItem> last(type: Class<T>): T? = lastOrNull { type.isInstance(it) } as? T
    override fun <T : MessageItem> find(type: Class<T>): List<T> = filter { type.isInstance(it) } as List<T>

    override fun toMessage() = Message(this)

    override fun unshift(item: MessageItem): MessageItemChain {
        first = ChainItem(item, first)
        _size++
        return this
    }

    override fun append(item: MessageItem): MessageItemChain {
        if (first == null) {
            val i = ChainItem(item)
            first = i
            last = i
            _size = 1
        } else {
            val i = ChainItem(item, null, last)
            last!!.next = i
            last = i
            _size++
        }
        return this
    }

    override fun append(chain: MessageItemChain): MessageItemChain {
        if (chain.first != null)
            if (first == null) {
                first = chain.first
                last = chain.last
                _size = chain._size
            } else {
                last!!.next = chain.first
                last = chain.last
                _size += chain._size
            }
        return this
    }

    override fun plus(item: MessageItem) = append(item)

    override fun plus(item: String) = TODO()

    override fun plus(item: Message) = append(item.body)

    override fun plus(item: MessageItemChain) = append(item)

    override fun contains(element: MessageItem): Boolean {
        var item = first
        while (item != null) if (item.item == element) return true else item = item.next
        return false
    }

    override fun containsAll(elements: Collection<MessageItem>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): MessageItem {
        if (index >= size) throw ArrayIndexOutOfBoundsException(index)
        if (index == 0) return first!!.item
        var item = first
        for (i in 1..index) {
            item = item!!.next
        }
        return item!!.item
    }


    override fun indexOf(element: MessageItem): Int {
        var item = first
        for (i in 0 until size) {
            if (item!!.item == element) return i else item = item.next
        }
        return -1
    }

    override fun isEmpty() = first == null

    class ChainIterator(private val chain: MessageItemChain, i: Int = 0) : MutableListIterator<MessageItem> {
        var index = 0
        var next = chain.first
        var previous: ChainItem? = null

        init {
            for (j in 1..i) next()
        }

        override fun hasNext() = next != null

        override fun nextIndex() = index
        override fun next(): MessageItem {
            val next = next ?: throw NoSuchElementException()

            val i = next.item
            this.index++
            this.previous = next
            this.next = next.next

            return i
        }

        override fun hasPrevious() = previous != null
        override fun previousIndex() = if (previous == null) -1 else index - 1
        override fun previous(): MessageItem {
            val previous = previous ?: throw NoSuchElementException()

            val i = previous.item
            this.index--
            this.next = previous
            this.previous = previous.previous

            return i
        }

        override fun add(element: MessageItem) {
            val chainItem = ChainItem(element, next, previous)
            if (next == null && previous == null) {
                chain._size = 1
                chain.first = chainItem
                chain.last = chainItem
            } else chain._size++
            next?.previous = chainItem
            previous?.next = chainItem
            next = chainItem
        }

        override fun remove() {
            if (next == null) return
            val next = next!!.next
            if (previous == null) chain.first = next
            if (next == null) chain.last = previous
            previous?.next = next
            next?.previous = previous
            chain._size -= 1
        }

        override fun set(element: MessageItem) {
            next!!.item = element
        }

    }

    override fun iterator(): MutableIterator<MessageItem> = ChainIterator(this)

    override fun lastIndexOf(element: MessageItem): Int {
        var item = first
        var index = -1
        for (i in 0 until size) {
            if (item!!.item == element) index = i else item = item.next
        }
        return index
    }

    override fun listIterator(): MutableListIterator<MessageItem> = ChainIterator(this)

    override fun listIterator(index: Int): MutableListIterator<MessageItem> = ChainIterator(this, index)

    override fun subList(fromIndex: Int, toIndex: Int): MessageItemChain {
        if (fromIndex < 0 || fromIndex > size) throw ArrayIndexOutOfBoundsException(fromIndex)
        if (toIndex < fromIndex || fromIndex > size) throw ArrayIndexOutOfBoundsException(toIndex)
        val list = MessageItemChain()
        var item = first
        for (i in 0..toIndex) {
            if (i in fromIndex..toIndex) list.append(item!!.item)
            item = item!!.next
        }
        return list
    }


}