package com.icecreamqaq.yuq.util

import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.firstString
import com.icecreamqaq.yuq.message.Message.Companion.toChain
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.Message.Companion.toMessageByRainCode
import com.icecreamqaq.yuq.message.Text.Companion.toText

class MessageUtil {

    companion object{

        @JvmStatic
        fun stringToText(string: String) = string.toText()

        @JvmStatic
        fun stringToChain(string: String) = string.toChain()

        @JvmStatic
        fun stringToMessageByRainCode(rainCodeString: String) = rainCodeString.toMessageByRainCode()

        @JvmStatic
        fun stringToMessage(string: String) = string.toMessage()

        @JvmStatic
        fun firstString(message: Message) = message.firstString()
    }
}