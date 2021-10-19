package com.icecreamqaq.yuq.rainCode

import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem

object RainCode {

    private val decoders: Map<String, RainCodeDecoder> = HashMap()


    @JvmStatic
    @JvmName("decodeRainCodeString")
    fun String.decodeRainCode(): Message {
        val message = Message()
        return message
    }

    @JvmStatic
    @JvmName("encodeMessage")
    fun Message.encode(): String {
        val sb = StringBuilder()
        return sb.toString()
    }

}

interface RainCodeDecoder {

    fun RainCodeItem.decode(): MessageItem

}

data class RainCodeItem(
    val namespace: String,
    val function: String,
    val paras: Map<String, Any>? = null,
    val actionContext: BotActionContext? = null
)