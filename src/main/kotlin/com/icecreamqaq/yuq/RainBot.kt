package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.router.NewRouter
import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.ContextRouter
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Friend
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.error.SendMessageFailedByCancel
import com.icecreamqaq.yuq.event.*
import com.icecreamqaq.yuq.job.RainInfo
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.MessageSource
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.resume

open class RainBot {

    private val log = LoggerFactory.getLogger(RainBot::class.java)

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    @field:Named("ContextSession")
    lateinit var sessionCache: EhcacheHelp<ContextSession>

    @Inject
    @field:Named("group")
    private lateinit var group: NewRouter

    @Inject
    @field:Named("priv")
    private lateinit var priv: NewRouter

    @Inject
    private lateinit var contextRouter: ContextRouter

    @Inject
    private lateinit var rainInfo: RainInfo


    @Deprecated("应该使用具体的 receiveFriendMessage 或是 receiveTempMessage")
    open fun receivePrivateMessage(sender: Contact, message: Message) = when (sender) {
        is Friend -> receiveFriendMessage(sender, message)
        is Member -> receiveTempMessage(sender, message)
        else -> error("Not A PrivateContact")
    }

    open fun receiveFriendMessage(sender: Friend, message: Message) {
        log.info("${sender.toLogString()} -> ${message.toLogString()}")
        rainInfo.receiveMessage()
        if (eventBus.post(PrivateMessageEvent.FriendMessage(sender, message))) return
        val context = BotActionContext(sender, sender, message, getContextSession(sender.id.toString()), 1)
        priv.todo(context)
    }

    open fun receiveTempMessage(sender: Member, message: Message) {
        log.info("${sender.toLogString()} -> ${message.toLogString()}")
        rainInfo.receiveMessage()
        if (eventBus.post(PrivateMessageEvent.TempMessage(sender, message))) return
        val context = BotActionContext(sender, sender, message, getContextSession(sender.id.toString()), 2)
        priv.todo(context)
    }

    open fun receiveGroupMessage(sender: Member, message: Message) {
        log.info("[${sender.group.toLogString()}]${sender.toLogStringSingle()} -> ${message.toLogString()}")
        rainInfo.receiveMessage()
        if (eventBus.post(GroupMessageEvent(sender, sender.group, message))) return
        val context = BotActionContext(sender.group, sender, message, getContextSession("${sender.group.id}_${sender.id}"), 0)
        group.todo(context)
    }

    open fun NewRouter.todo(context: BotActionContext) {
        if (context.path.isEmpty()) return
        if (context.session.suspendCoroutineIt != null) {
            context.session.suspendCoroutineIt!!.resume(context.message)
        }
        if (eventBus.post(ActionContextInvokeEvent.Per(context))) return
        val session = context.session
        if (session.context != null) contextRouter.invoke(session.context!!, context)
        else this.invoke(context.path[0], context)
        if (eventBus.post(ActionContextInvokeEvent.Post(context))) return
        session.context = context.nextContext?.router
        if (context.nextContext != null) {
            val msg = contextRouter.routers[context.nextContext?.router]?.tips?.get(context.nextContext?.status)
            if (msg != null) context.source.sendMessage(msg.toMessage())
        }
        context.source.sendMessage(context.reMessage ?: return)
    }

    fun <T> sendMessage(message: Message, contact: Contact, obj: T, send: (T) -> MessageSource): MessageSource {
        val ms = message.toLogString()
        val ts = contact.toLogString()
        log.debug("Send Message To: $ts, $ms")
        return SendMessageEvent.Per(contact, message)(
                {
                    val m = send(obj)
                    log.info("$ts <- $ms")
                    rainInfo.sendMessage()
                    SendMessageEvent.Post(contact, message, m)
                    m
                },
                { throw SendMessageFailedByCancel() }
        )!!
    }

    open fun getContextSession(sessionId: String) = sessionCache[sessionId] ?: {
        val session = ContextSession(sessionId)
        eventBus.post(ContextSessionCreateEvent(session))
        sessionCache[sessionId] = session
        session
    }()

//    open fun


}