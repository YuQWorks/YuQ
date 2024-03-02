package com.icecreamqaq.yuq.error

import com.icecreamqaq.yuq.message.Message

open class YuQException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
open class YuQRuntimeException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

open class ImageTypedException(message: String) : YuQRuntimeException(message)

open class WaitNextMessageTimeoutException : YuQRuntimeException()

open class SendMessageFailedByCancel : YuQRuntimeException("消息发送失败，发送消息被拦截。")
open class SendMessageFailedByTimeout : YuQRuntimeException("消息发送完成，但是接收消息失败，可能被服务端拒绝广播。")

open class PermissionDeniedException : YuQRuntimeException("权限不足。")

class MessageThrowable(val c: Message) : RuntimeException()