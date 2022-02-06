package test.yuq

import com.IceCreamQAQ.Yu.`as`.ApplicationService
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.event.EventBus
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.*
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.message.MessageItemFactory
import test.yuq.event.RegisterContactEvent
import test.yuq.message.MessageItemFactoryImpl
import javax.inject.Inject

class TestBot : YuQ, ApplicationService, User, YuQVersion {
    override val avatar: String = ""
    override val id: Long = 25331213
    override val platformId: String = id.toString()
    override val name: String = "测试用 Bot"
    override fun canSendMessage() = false
    override fun isFriend() = false

    override fun runtimeName() = "YuQDevTools"
    override fun runtimeVersion() = apiVersion()

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    lateinit var rainBot: YuQInternalBotImpl

    @Inject
    private lateinit var context: YuContext

    override fun init() {
        mif = messageItemFactory
        com.icecreamqaq.yuq.eventBus = eventBus
        yuq = this
        com.icecreamqaq.yuq.web = web

    }

    override fun start() {
        context.injectBean(rainBot)
        RegisterContactEvent(friends, groups).post()
    }

    override fun stop() {
    }

    override val botId: Long = id
    override val botInfo: User = this
    override val cookieEx: YuQ.QQCookie
        get() = TODO("Not yet implemented")
    override val friends: UserList<Friend> = UserListImpl()
    override val groups: UserList<Group> = UserListImpl()
    override val messageItemFactory: MessageItemFactory = MessageItemFactoryImpl()

    @Inject
    override lateinit var web: Web

    override fun refreshFriends(): UserList<Friend> {
        TODO("Not yet implemented")
    }

    override fun refreshGroups(): UserList<Group> {
        TODO("Not yet implemented")
    }

    override fun id2platformId(id: Long): String = id.toString()

    override fun platformId2id(platformId: String) = platformId.toLong()
}