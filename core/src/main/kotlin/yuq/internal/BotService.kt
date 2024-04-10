package yuq.internal

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rain.api.event.EventBus
import rain.di.Config
import rain.function.slf4j
import yuq.Bot
import yuq.annotation.Internal
import yuq.contact.Contact
import yuq.contact.Friend
import yuq.contact.GroupMember
import yuq.controller.BotActionContext
import yuq.controller.MessageChannel
import yuq.error.SendMessageFailedByCancel
import yuq.error.SendMessageFailedByTimeout
import yuq.event.AtBotEvent
import yuq.event.MessageEvent
import yuq.event.SendMessageEvent
import yuq.message.Message
import yuq.message.MessageSource
import yuq.message.items.At
import yuq.message.items.Text
import yuq.message.source.MessageFailByCancel
import yuq.message.source.MessageFailByReadTimeOut

@Internal
class BotService(
    val eventBus: EventBus,
    @Config("bot.name") val botName: String? = null,
    @Config("yuq.strict") val strict: Boolean,
    val frameworkInfo: FrameworkInfo,
) {

    companion object {
        val log = slf4j()
    }

    private fun Message.getOnlyAtFlag(bot: Bot): Int {
        if (body.size > 1) return 0
        val item = body[0]
        if (item is At && item.target == bot.id) return 1
        if (botName != null && item is Text && botName == item.text) return 2
        return 0
    }

    suspend fun receiveFriendMessage(bot: Bot, sender: Friend, message: Message) {
        log.info("${sender.logString} -> ${message.toLogString()}")
        frameworkInfo.receiveMessage(bot.guid)
        if (eventBus.post(MessageEvent.PrivateMessage.FriendMessage(sender, message))) return
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag(bot)
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

    suspend fun receiveTempMessage(bot: Bot, sender: GroupMember, message: Message) {
        log.info("${sender.logString} -> ${message.toLogString()}")
        frameworkInfo.receiveMessage(bot.guid)
        if (eventBus.post(MessageEvent.PrivateMessage.TempMessage(sender, message))) return
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag(bot)
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

    suspend fun receiveGroupMessage(bot: Bot, sender: GroupMember, message: Message) {
        log.info("[${sender.group.logString}]${sender.logStringSingle} -> ${message.toLogString()}")
        frameworkInfo.receiveMessage(bot.guid)
        if (eventBus.post(MessageEvent.GroupMessage(sender, sender.group, message))) return
//        val groupSession = botService.getContextSession(bot, "g${sender.group.id}")
//        if (groupSession.suspendCoroutineIt != null) {
//            groupSession.suspendCoroutineIt!!.complete(message)
//            return
//        }
        if (message.body.isEmpty()) return
        val flag = message.getOnlyAtFlag(bot)
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

    suspend fun doRouter(context: BotActionContext){

    }

    suspend fun hookSendMessage(
        message: Message,
        contact: Contact,
        sendFun: suspend () -> MessageSource?
    ): MessageSource {
        val mLog = message.toLogString()
        val cLog = contact.logString

        log.debug("准备发送消息: $cLog, $mLog")
        if (eventBus.post(SendMessageEvent.Per(contact, message))) return messageSendFailedByCancel(contact, message)
        val ms = sendFun() ?: messageSendFailedByReadTimeout(contact, message)
        log.info("$cLog <- $mLog")
        eventBus.post(SendMessageEvent.Post(contact, message, ms))
        message.recallDelay?.let {
            coroutineScope {
                launch {
                    delay(it)
                    ms.recall()
                }
            }
        }
        return ms
    }

    fun messageSendFailedByCancel(contact: Contact, message: Message): MessageSource {
        if (strict) throw SendMessageFailedByCancel()
        return MessageFailByCancel()
    }

    fun messageSendFailedByReadTimeout(contact: Contact, message: Message): MessageSource {
        if (strict) throw SendMessageFailedByTimeout()
        return MessageFailByReadTimeOut()
    }
}