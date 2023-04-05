package com.icecreamqaq.yuq.contact

import com.icecreamqaq.yuq.error.WaitNextMessageTimeoutException
import com.icecreamqaq.yuq.message.Message
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

/*** 联系人上下文会话
 * 该对象描述一个联系人的上下文会话，可以用来保存会话数据。
 * 上下文对象将保存一段时间，若联系人一段时间内没有发送消息，上下文对象将被销毁。
 */
class ContactSession(val id: String, private val saves: MutableMap<String, Any> = ConcurrentHashMap()) {

    internal var suspendCoroutineIt: CompletableDeferred<Message>? = null

    /*** 获取会话数据
     * @param name 数据名
     * @return 返回数据
     */
    operator fun get(name: String) = saves[name]
    /*** 设置会话数据
     * @param name 数据名
     * @param value 数据
     */
    operator fun set(name: String, value: Any) {
        saves[name] = value
    }
    /*** 移除会话数据
     * @param name 数据名
     * @return 返回数据
     */
    fun remove(name: String) = saves.remove(name)

    /*** 等待下一条消息
     * 联系人的下一条消息将被捕获。
     * 被捕获的消息不会再进入 Controller 链路处理流程。
     * 但依旧会执行消息事件，若消息事件被取消，则不会捕获本条消息，将会继续等待。
     *
     * @param maxTime 最大等待时间，单位毫秒，默认为 30000 毫秒。
     * @return 返回下一条消息。
     * @throws WaitNextMessageTimeoutException 等待超时时抛出该异常。
     */
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