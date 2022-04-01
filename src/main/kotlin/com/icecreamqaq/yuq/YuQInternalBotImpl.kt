package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.controller.Router
import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.ContextRouter
import com.icecreamqaq.yuq.controller.ContextSession
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.error.SendMessageFailedByCancel
import com.icecreamqaq.yuq.event.*
import com.icecreamqaq.yuq.job.YuQRunningInfo
import com.icecreamqaq.yuq.message.At
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.Message.Companion.toMessage
import com.icecreamqaq.yuq.message.MessageSource
import com.icecreamqaq.yuq.message.Text
import com.icecreamqaq.yuq.util.YuQInternalFun
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named

open class YuQInternalBotImpl {

    private val log = LoggerFactory.getLogger(YuQInternalBotImpl::class.java)

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    @field:Named("ContextSession")
    lateinit var sessionCache: EhcacheHelp<ContextSession>

    @Inject
    @field:Named("group")
    private lateinit var group: Router

    @Inject
    @field:Named("priv")
    private lateinit var priv: Router

    @Inject
    @field:Named("guild")
    private lateinit var guild: Router

    @Inject
    private lateinit var contextRouter: ContextRouter

    @Inject
    private lateinit var runningInfo: YuQRunningInfo

    @Inject
    private lateinit var internalFun: YuQInternalFun

    @Config("YuQ.bot.name")
    private var botName: String? = null

    data class RainCodeConfig(
        val prefix: String = "^",
        val enable: Boolean = false
    )

    @Inject
    fun init() {
        internalBot = this
    }

    private fun Message.getOnlyAtFlag(): Int {
        if (body.size > 1) return 0

        val i = body[0]
        if (i is At) if (i.user == yuq.botId) return 1
        if (botName != null) if (i is Text) if (i.text == botName) return 2
        return 0
    }

    @Config("YuQ.Controller.RainCode")
    lateinit var rainCode: RainCodeConfig

    open suspend fun receiveFriendMessage(sender: Friend, message: Message) {
        log.info("${sender.toLogString()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
        if (eventBus.post(PrivateMessageEvent.FriendMessage(sender, message))) return
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag()
        if (flag > 0) {
            eventBus.post(AtBotEvent.ByPrivate.ByFriend(flag, sender, sender))
            return
        }
        val context = BotActionContext(sender, sender, message, getContextSession(sender.id.toString()), null, 1)
        priv.todo(context)
    }

    open suspend fun receiveTempMessage(sender: Member, message: Message) {
        log.info("${sender.toLogString()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
        if (eventBus.post(PrivateMessageEvent.TempMessage(sender, message))) return
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag()
        if (flag > 0) {
            eventBus.post(AtBotEvent.ByPrivate.ByTemp(flag, sender, sender))
            return
        }
        val context = BotActionContext(sender, sender, message, getContextSession(sender.id.toString()), null, 2)
        priv.todo(context)
    }

    open suspend fun receiveGroupMessage(sender: Member, message: Message) {
        log.info("[${sender.group.toLogString()}]${sender.toLogStringSingle()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
        internalFun.setMemberLastMessageTime(sender, System.currentTimeMillis())
        if (eventBus.post(GroupMessageEvent(sender, sender.group, message))) return
        val groupSession = internalBot.getContextSession("g${sender.group.id}")
        if (groupSession.suspendCoroutineIt != null) {
            groupSession.suspendCoroutineIt!!.complete(message)
            return
        }
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag()
        if (flag > 0) {
            eventBus.post(AtBotEvent.ByGroup(flag, sender, sender.group))
            return
        }
        val context =
            BotActionContext(
                sender.group,
                sender,
                message,
                getContextSession("${sender.group.id}_${sender.id}"),
                groupSession,
                0
            )
        group.todo(context)
    }

    open suspend fun receiveGuildMessage(channel: Channel, sender: GuildMember, message: Message) {
        log.info("[${channel.toLogString()}]${sender.toLogString()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
//        internalFun.setMemberLastMessageTime(sender, System.currentTimeMillis())
        if (eventBus.post(GuildMessageEvent(sender, channel.guild, channel, message))) return
        val channelSession = internalBot.getContextSession(channel.guid)
        if (channelSession.suspendCoroutineIt != null) {
            channelSession.suspendCoroutineIt!!.complete(message)
            return
        }
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag()
        if (flag > 0) {
            eventBus.post(AtBotEvent.ByGuild(flag, sender, channel, channel.guild))
            return
        }
        val context =
            BotActionContext(
                channel,
                sender,
                message,
                getContextSession("gcm${channel.guid}_${sender.guid}"),
                channelSession,
                0
            )
        guild.todo(context)
    }

    open suspend fun Router.todo(context: BotActionContext) {
        if (context.path.isEmpty()) return
        kotlin.runCatching {
            if (context.session.suspendCoroutineIt != null) {
                context.session.suspendCoroutineIt!!.complete(context.message)
                return
            }
            if (eventBus.post(ActionContextInvokeEvent.Per(context))) return
            val session = context.session
            val flag = if (session.context != null) contextRouter.invoke(session.context!!, context)
            else this.invoke(context.path[0], context)
            if (eventBus.post(ActionContextInvokeEvent.Post(context, flag))) return
            session.context = context.nextContext?.router
            if (context.nextContext != null) {
                val msg = contextRouter.routers[context.nextContext?.router]?.tips?.get(context.nextContext?.status)
                if (msg != null) context.source.sendMessage(msg.toMessage())
            }
        }.onFailure {
            it.printStackTrace()
            return
        }
        val source = context.source.sendMessage(context.reMessage ?: return)
        (context.recall ?: context.reMessage!!.recallDelay)?.let {
            coroutineScope {
                delay(it)
                source.recall()
            }
        }
    }

    fun <T, R : MessageSource> sendMessage(
        message: Message,
        contact: Contact,
        obj: T,
        send: (T) -> R
    ): R {
        val ms = message.toLogString()
        val ts = contact.toLogString()
        log.debug("Send Message To: $ts, $ms")

        SendMessageEvent.Per(contact, message)(SendMessageFailedByCancel())
        val m = send(obj)
        log.info("$ts <- $ms")
        runningInfo.sendMessage()
        SendMessageEvent.Post(contact, message, m)()
        message.recallDelay?.let {
            GlobalScope.launch {
                delay(it)
                m.recall()
            }
        }
        return m
    }

    open fun getContextSession(sessionId: String) = sessionCache[sessionId] ?: run {
        val session = ContextSession(sessionId)
        eventBus.post(ContextSessionCreateEvent(session))
        sessionCache[sessionId] = session
        session
    }

//    open fun

//    data class Spx
}