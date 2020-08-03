package com.icecreamqaq.yuq.util

import com.icecreamqaq.yuq.firstString
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.toMessage
import com.icecreamqaq.yuq.toText

class MessageUtil {

    companion object{

        @JvmStatic
        fun stringToText(string: String) = string.toText()

        @JvmStatic
        fun stringToMessage(string: String) = string.toMessage()

        @JvmStatic
        fun firstString(message: Message) = message.firstString()
    }
}