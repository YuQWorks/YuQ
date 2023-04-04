package com.icecreamqaq.yuq.entity

import com.icecreamqaq.yuq.error.WaitNextMessageTimeoutException
import com.icecreamqaq.yuq.message.Message
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


class ContactSession(val id: String, private val saves: MutableMap<String, Any> = ConcurrentHashMap()) {

    var suspendCoroutineIt: CompletableDeferred<Message>? = null
    var context: String? = null

    operator fun get(name: String) = saves[name]
    operator fun set(name: String, value: Any) {
        saves[name] = value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContactSession

        if (id != other.id) return false
        if (saves != other.saves) return false
        return context == other.context
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + saves.hashCode()
        result = 31 * result + (context?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ContextSession(id='$id', saves=$saves, context=$context)"
    }


    suspend fun waitNextMessage(maxTime: Long = 30000): Message =
        try {
            suspendCoroutineIt = CompletableDeferred()
            withTimeout(maxTime) { suspendCoroutineIt!!.await() }
        } catch (e: Exception) {
            throw WaitNextMessageTimeoutException()
        } finally {
            suspendCoroutineIt = null
        }


}