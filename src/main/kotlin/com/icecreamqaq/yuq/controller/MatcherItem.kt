package com.icecreamqaq.yuq.controller

import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.message.MessageItemChain
import com.icecreamqaq.yuq.message.Text

class MatcherItem(private val items: MessageItemChain) {

    var end = false

    var currentItemIndex = 0
    var currentItemMatchIndex = 0

    var currentItem: MessageItem = items[0]
    var currentString: String? = (currentItem as? Text)?.text

    fun changeItem(index: Int) {
        if (index >= items.size) {
            end = true
            return
        }
        currentItem = items[index]
        currentString = (currentItem as? Text)?.text
        currentItemMatchIndex = 0
    }

    fun nextItem() {
        changeItem(currentItemIndex + 1)
    }

    fun mark() = currentItemIndex to currentItemMatchIndex

    fun reMark(mark: Pair<Int, Int>) {
        changeItem(mark.first)

        currentItemMatchIndex = mark.second
    }

    fun isNext(item: String): Boolean {
        if (end) return false
        if (currentString == null) return false
        if (item.length > currentString!!.length - currentItemMatchIndex) return false
        if (currentString!!.substring(currentItemMatchIndex, currentItemMatchIndex + item.length) == item) {
            currentItemMatchIndex += item.length

            if (currentItemMatchIndex == currentString!!.length) nextItem()

            return true
        }

        return true
    }
}