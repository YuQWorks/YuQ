package yuq.controller

import yuq.message.MessageItem
import yuq.message.chain.MessageBody
import yuq.message.items.Text

class MatcherItem(private val items: MessageBody) {

    var end = items.isEmpty()
        private set
    var currentItemIndex = 0
        private set
    var currentItemMatchIndex = 0
        private set
    var currentItem: MessageItem = items.firstOrNull() ?: Text("")
        private set
    var currentString: String? = (currentItem as? Text)?.text
        private set

    fun changeItem(index: Int) {
        currentItemIndex = index
        currentItemMatchIndex = 0
        end = index >= items.size
        currentItem = if (end) Text("") else items[index]
        currentString = (currentItem as? Text)?.text
    }

    fun nextItem() = changeItem(currentItemIndex + 1)

    fun mark() = currentItemIndex to currentItemMatchIndex

    fun reMark(mark: Pair<Int, Int>) {
        changeItem(mark.first)
        currentItemMatchIndex = mark.second
    }

    private fun normalize() {
        while (!end && currentString != null && currentItemMatchIndex >= currentString!!.length) nextItem()
    }

    fun skipWhitespace() {
        normalize()
        while (!end && currentString?.get(currentItemMatchIndex)?.isWhitespace() == true) {
            currentItemMatchIndex++
            normalize()
        }
    }

    fun isNext(item: String): Boolean {
        val start = mark()
        for (character in item) {
            normalize()
            if (end || currentString?.get(currentItemMatchIndex) != character) {
                reMark(start)
                return false
            }
            currentItemMatchIndex++
        }
        normalize()
        return true
    }

    fun isBoundary(): Boolean {
        normalize()
        return end || currentString == null || currentString!![currentItemMatchIndex].isWhitespace()
    }

    fun readArgument(): Any? {
        skipWhitespace()
        if (end) return null
        if (currentString == null) return currentItem.also { nextItem() }
        val result = StringBuilder()
        while (!end && currentString != null && !currentString!![currentItemMatchIndex].isWhitespace()) {
            result.append(currentString!![currentItemMatchIndex++])
            normalize()
        }
        return result.toString()
    }

    fun hasRemaining(): Boolean {
        skipWhitespace()
        return !end
    }
}
