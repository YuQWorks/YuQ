package yuq.controller

import yuq.message.MessageItem
import yuq.message.chain.MessageBody
import yuq.message.items.Text

/** A cursor over message items. Text offsets never replace non-text items. */
class MatcherItem private constructor(private val items: List<MessageItem>) {
    constructor(body: MessageBody) : this(body.toList())
    var currentItemIndex = 0
        private set
    var currentItemMatchIndex = 0
        private set

    val end: Boolean get() {
        normalize()
        return currentItemIndex == items.size
    }
    val currentItem: MessageItem get() {
        normalize()
        return items.getOrNull(currentItemIndex) ?: Text("")
    }
    val currentString: String? get() = (currentItem as? Text)?.text

    private fun normalize() {
        while (currentItemIndex < items.size) {
            val text = items[currentItemIndex] as? Text ?: return
            if (currentItemMatchIndex < text.text.length) return
            currentItemIndex++
            currentItemMatchIndex = 0
        }
    }

    fun changeItem(index: Int) {
        require(index in 0..items.size)
        currentItemIndex = index
        currentItemMatchIndex = 0
        normalize()
    }

    fun nextItem() = changeItem(currentItemIndex + 1)

    fun mark(): Pair<Int, Int> {
        normalize()
        return currentItemIndex to currentItemMatchIndex
    }

    fun reMark(mark: Pair<Int, Int>) {
        require(mark.first in 0..items.size)
        val length = (items.getOrNull(mark.first) as? Text)?.text?.length ?: 0
        require(mark.second in 0..length)
        currentItemIndex = mark.first
        currentItemMatchIndex = mark.second
        normalize()
    }

    /** Consume a literal across adjacent Text items; failure is atomic. */
    fun isNext(text: String): Boolean {
        val start = mark()
        for (character in text) {
            if (end || currentString?.get(currentItemMatchIndex) != character) {
                reMark(start)
                return false
            }
            currentItemMatchIndex++
            normalize()
        }
        return true
    }

    fun skipWhitespace() {
        while (!end && currentString?.get(currentItemMatchIndex)?.isWhitespace() == true) {
            currentItemMatchIndex++
            normalize()
        }
    }

    fun hasRemaining(): Boolean = !end

    /** Handlers inspect an independent cursor and report candidate ends to the route engine. */
    fun fork(): MatcherItem = MatcherItem(items).also { it.reMark(mark()) }

    fun remainingText(): String {
        val start = mark()
        val text = StringBuilder()
        for (index in start.first until items.size) {
            val item = items[index] as? Text ?: break
            text.append(item.text, if (index == start.first) start.second else 0, item.text.length)
        }
        return text.toString()
    }

    fun <T> at(position: Pair<Int, Int>, inspect: () -> T): T {
        val start = mark()
        try {
            reMark(position)
            return inspect()
        } finally { reMark(start) }
    }

    /** Every possible consumption boundary, including boundaries around rich items. */
    fun boundaries(from: Pair<Int, Int> = mark()): Sequence<Pair<Int, Int>> = sequence {
        for (index in from.first until items.size) {
            val item = items[index]
            if (item is Text) {
                val start = if (index == from.first) from.second else 0
                for (offset in start + 1..item.text.length) {
                    // A String parameter must not split a surrogate pair.
                    if (offset < item.text.length && item.text[offset - 1].isHighSurrogate() &&
                        item.text[offset].isLowSurrogate()) continue
                    yield(index to offset)
                }
            } else {
                yield(index + 1 to 0)
            }
        }
    }

    fun atEnd(position: Pair<Int, Int>): Boolean {
        val start = mark()
        reMark(position)
        val result = end
        reMark(start)
        return result
    }

    fun startsWith(position: Pair<Int, Int>, text: String, skipGap: Boolean = false): Boolean {
        val start = mark()
        reMark(position)
        if (skipGap) skipWhitespace()
        val result = isNext(text)
        reMark(start)
        return result
    }

    /** Copy the consumed range; original item objects and the input chain stay intact. */
    fun capture(from: Pair<Int, Int>, to: Pair<Int, Int>): MessageBody {
        require(to.first > from.first || to.first == from.first && to.second >= from.second)
        val result = MessageBody()
        for (index in from.first..minOf(to.first, items.lastIndex)) {
            val item = items[index]
            if (item is Text) {
                val start = if (index == from.first) from.second else 0
                val finish = if (index == to.first) to.second else item.text.length
                if (finish > start) result.append(
                    if (start == 0 && finish == item.text.length) item else Text(item.text.substring(start, finish))
                )
            } else if (index < to.first) {
                result.append(item)
            }
        }
        return result
    }
}
