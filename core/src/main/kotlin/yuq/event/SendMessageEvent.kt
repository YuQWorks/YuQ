package yuq.event

import rain.event.events.CancelAbleEvent
import yuq.Bot
import yuq.contact.Contact
import yuq.message.Message
import yuq.message.MessageSource

open class SendMessageEvent(val sendTo: Contact, val message: Message) : BotEvent {
    open class Per(sendTo: Contact, message: Message) : SendMessageEvent(sendTo, message), CancelAbleEvent {
        override var isCanceled: Boolean = false
    }

    open class Post(sendTo: Contact, message: Message, val messageSource: MessageSource) :
        SendMessageEvent(sendTo, message)

    override val bot: Bot
        get() = sendTo.bot
}