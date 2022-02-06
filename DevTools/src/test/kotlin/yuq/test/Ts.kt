package yuq.test

import com.IceCreamQAQ.Yu.annotation.Event
import com.IceCreamQAQ.Yu.annotation.EventListener
import com.IceCreamQAQ.Yu.event.events.AppStartEvent
import com.icecreamqaq.yuq.YuQInternalBotImpl
import com.icecreamqaq.yuq.message.Message
import kotlinx.coroutines.runBlocking
import test.yuq.contcat.FriendImpl
import test.yuq.event.RegisterContactEvent
import test.yuq.message.make
import javax.inject.Inject

@EventListener
class Ts {

    @Inject
    private lateinit var rainBot: YuQInternalBotImpl

    @Event
    fun registerEvent(e:RegisterContactEvent){

    }

    @Event
    fun appStartEvent(e:AppStartEvent){
        runBlocking {
            rainBot.receiveFriendMessage(FriendImpl(123132456,"测试"), (Message() + "HelloWorld").make())
        }
    }

}