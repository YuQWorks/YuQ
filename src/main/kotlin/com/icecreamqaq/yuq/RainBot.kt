package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.router.NewRouter
import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.ContextRouter
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.entity.Contact
import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.event.ActionContextInvokeEvent
import com.icecreamqaq.yuq.event.ContextSessionCreateEvent
import com.icecreamqaq.yuq.event.GroupMessageEvent
import com.icecreamqaq.yuq.event.PrivateMessageEvent
import com.icecreamqaq.yuq.message.Message
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named

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


    open fun receivePrivateMessage(sender: Contact, message: Message) {
        log.info("Receive Private Message: $sender, $message")
        if (eventBus.post(PrivateMessageEvent(sender, message))) return
        val context = BotActionContext(sender, sender, message, getContextSession(sender.id.toString()))
        priv.todo(context)
    }

    open fun receiveGroupMessage(sender: Member, message: Message) {
        log.info("Receive Group Message: $sender, $message")
        if (eventBus.post(GroupMessageEvent(sender, sender.group, message))) return
        val context = BotActionContext(sender.group, sender, message, getContextSession("${sender.group.id}_${sender.id}"))
        group.todo(context)
    }

    open fun NewRouter.todo(context: BotActionContext) {
        if (context.path.isEmpty()) return
        if (eventBus.post(ActionContextInvokeEvent.Per(context))) return
        val session = context.session
        if (session.context != null) contextRouter.invoke(session.context!!, context)
        else this.invoke(context.path[0], context)
        if (eventBus.post(ActionContextInvokeEvent.Post(context))) return
        if (session.context != null) {
            val msg = contextRouter.routers[session.context!!]?.tips?.get(context.nextContext?.status)
            if (msg != null) context.source.sendMessage(msg.toMessage())
        }
        context.source.sendMessage(context.reMessage ?: return)
    }

    open fun getContextSession(sessionId: String) = sessionCache[sessionId] ?: {
        val session = ContextSession(sessionId)
        eventBus.post(ContextSessionCreateEvent(session))
        sessionCache[sessionId] = session
        session
    }()

//    open fun


}