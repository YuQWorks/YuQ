package yuq.test

import com.IceCreamQAQ.Yu.annotation.Event
import com.IceCreamQAQ.Yu.annotation.EventListener
import com.IceCreamQAQ.Yu.event.events.AppStartEvent
import com.icecreamqaq.yuq.BotService
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@EventListener
class Ts {

    @Inject
    private lateinit var rainBot: BotService


    @Event
    fun appStartEvent(e:AppStartEvent){
        runBlocking {
//            rainBot.receiveFriendMessage(FriendImpl(123132456,"测试"), (Message() + "HelloSuspend").make())
//            rainBot.receiveFriendMessage(FriendImpl(123132456,"测试"), (Message() + "HelloKotlin").make())
//            rainBot.receiveFriendMessage(FriendImpl(123132456,"测试"), (Message() + "HelloJava").make())
        }
    }

}