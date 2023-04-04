package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.cache.EhcacheHelp
import com.IceCreamQAQ.Yu.event.EventBus
import com.icecreamqaq.yuq.controller.BotActionContext
import com.icecreamqaq.yuq.controller.MessageChannel
import com.icecreamqaq.yuq.controller.router.BotRootRouter
import com.icecreamqaq.yuq.entity.ContactSession
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.error.SendMessageFailedByCancel
import com.icecreamqaq.yuq.error.SendMessageFailedByTimeout
import com.icecreamqaq.yuq.event.*
import com.icecreamqaq.yuq.job.YuQRunningInfo
import com.icecreamqaq.yuq.message.*
import com.icecreamqaq.yuq.util.YuQInternalFun
import com.icecreamqaq.yuq.util.liteMessage
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named

open class BotService(
    val rootRouter: BotRootRouter,
    val eventBus: EventBus,
    val runningInfo: YuQRunningInfo,
    val internalFun: YuQInternalFun,
    @Named("ContextSession")
    val sessionCache: EhcacheHelp<ContactSession>,
    @Config("YuQ.bot.name")
    val botName: String,
    @Config("yuq.chat.strict")
    val strict: Boolean,
    @Config("YuQ.Controller.RainCode")
    val rainCode: RainCodeConfig
) {

    companion object {
        private val log = LoggerFactory.getLogger(BotService::class.java)
    }

    data class RainCodeConfig(
        val prefix: String = "^",
        val enable: Boolean = false
    )

    @Inject
    fun init() {
        botService = this
    }

    private fun Message.getOnlyAtFlag(): Int {
        if (body.size > 1) return 0

        val i = body[0]
        if (i is At) if (i.user == yuq.botId) return 1
        if (botName != null) if (i is Text) if (i.text == botName) return 2
        return 0
    }

    open suspend fun receiveFriendMessage(bot: Bot, sender: Friend, message: Message) {
        log.info("${sender.toLogString()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
        if (eventBus.post(PrivateMessageEvent.FriendMessage(sender, message))) return
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag()
        if (flag > 0) {
            eventBus.post(AtBotEvent.ByPrivate.ByFriend(flag, sender, sender))
            return
        }
        sender.session.suspendCoroutineIt?.let {
            it.complete(message)
            return
        }
        doRouter(BotActionContext(bot, MessageChannel.Friend, sender, sender, message))
    }

    open suspend fun receiveTempMessage(bot: Bot, sender: Member, message: Message) {
        log.info("${sender.toLogString()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
        if (eventBus.post(PrivateMessageEvent.TempMessage(sender, message))) return
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag()
        if (flag > 0) {
            eventBus.post(AtBotEvent.ByPrivate.ByTemp(flag, sender, sender))
            return
        }
        sender.session.suspendCoroutineIt?.let {
            it.complete(message)
            return
        }
        doRouter(BotActionContext(bot, MessageChannel.GroupTemporary, sender, sender, message))
    }

    open suspend fun receiveGroupMessage(bot: Bot, sender: Member, message: Message) {
        log.info("[${sender.group.toLogString()}]${sender.toLogStringSingle()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
        internalFun.setMemberLastMessageTime(sender, System.currentTimeMillis())
        if (eventBus.post(GroupMessageEvent(sender, sender.group, message))) return
        val groupSession = botService.getContextSession(bot, "g${sender.group.id}")
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
        sender.group.session.suspendCoroutineIt?.let {
            it.complete(message)
            return
        }
        sender.session.suspendCoroutineIt?.let {
            it.complete(message)
            return
        }
        doRouter(BotActionContext(bot, MessageChannel.Group, sender, sender.group, message))
    }

    open suspend fun receiveGuildMessage(bot: Bot, channel: Channel, sender: GuildMember, message: Message) {
        log.info("[${channel.toLogString()}]${sender.toLogString()} -> ${message.toLogString()}")
        runningInfo.receiveMessage()
        if (eventBus.post(GuildMessageEvent(sender, channel.guild, channel, message))) return
        val channelSession = botService.getContextSession(bot, channel.guid)
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
        channel.session.suspendCoroutineIt?.let {
            it.complete(message)
            return
        }
        sender.session.suspendCoroutineIt?.let {
            it.complete(message)
            return
        }
        doRouter(BotActionContext(bot, MessageChannel.Guild, sender, channel, message))
    }

    open suspend fun doRouter(context: BotActionContext) {
//        if (context.path.isEmpty()) return
        kotlin.runCatching {
            if (eventBus.post(ActionContextInvokeEvent.Per(context))) return
            val flag = rootRouter.router(context)
            if (eventBus.post(ActionContextInvokeEvent.Post(context, flag))) return
        }.onFailure {
            it.printStackTrace()
            return
        }
        context.result?.let { context.source.sendMessage(it as Message) }
    }

    suspend fun <T, R : MessageSource> sendMessage(
        message: Message,
        contact: Contact,
        obj: T,
        send: (T) -> R
    ): MessageSource {
        val ms = message.toLogString()
        val ts = contact.toLogString()
        log.debug("Send Message To: $ts, $ms")

        if (SendMessageEvent.Per(contact, message).post()) return messageSendFailedByCancel(contact, message)
        val m = send(obj)
        log.info("$ts <- $ms")
        runningInfo.sendMessage()
        SendMessageEvent.Post(contact, message, m)()
        message.recallDelay?.let {
            asyncDelay(it) {
                m.recall()
            }
        }
        return m
    }

    open fun messageSendFailedByCancel(contact: Contact, message: Message): MessageSource {
        SendMessageInvalidEvent.ByCancel(contact, message).post()
        if (strict) throw SendMessageFailedByCancel()
        return MessageFailByCancel.create(contact, message.liteMessage)
    }

    open fun messageSendFailedByReadTimeout(contact: Contact, message: Message): MessageSource {
        SendMessageInvalidEvent.ByReadTimeout(contact, message).post()
        if (strict) throw SendMessageFailedByTimeout()
        return MessageFailByReadTimeOut.create(contact, message.liteMessage)
    }

    open fun getContextSession(bot: Bot, sessionId: String) =
        sessionCache.getOrPut(
            sessionId,
            ContactSession(sessionId).apply { ContextSessionCreateEvent(bot, this).post() }
        )
//    sessionCache[sessionId] ?: run
//    {
//        val session = ContextSession(sessionId)
//        eventBus.post(ContextSessionCreateEvent(session))
//        sessionCache[sessionId] = session
//        session
//    }

//    open fun

//    data class Spx
}