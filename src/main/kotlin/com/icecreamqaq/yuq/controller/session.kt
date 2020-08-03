package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.annotation.Action
import com.IceCreamQAQ.Yu.annotation.After
import com.IceCreamQAQ.Yu.annotation.Before
import com.IceCreamQAQ.Yu.annotation.With
import com.IceCreamQAQ.Yu.controller.NewActionContext
import com.IceCreamQAQ.Yu.controller.router.NewMethodInvoker
import com.IceCreamQAQ.Yu.controller.router.NewRouter
import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.loader.LoadItem
import com.icecreamqaq.yuq.annotation.ContextTip
import com.icecreamqaq.yuq.annotation.ContextTips
import com.icecreamqaq.yuq.error.WaitNextMessageTimeoutException
import com.icecreamqaq.yuq.message.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ContextRouter {

    val routers: MutableMap<String, ContextAction> = ConcurrentHashMap()

    fun invoke(path: String, context: NewActionContext) = this.routers[path]?.invoker?.invoke("", context) ?: false
}

data class ContextAction(val invoker: NewRouter, val tips: Map<Int, String>)

class BotContextControllerLoader : BotControllerLoader() {

    @Inject
    private lateinit var context: YuContext

    override fun load(items: Map<String, LoadItem>) {
        val router = ContextRouter()
        for (item in items.values) {
            val instance = context[item.type] ?: continue
            controllerToRouter(instance, router)
        }
        context.putBean(ContextRouter::class.java, "", router)
    }

    private fun controllerToRouter(instance: Any, rootRouter: ContextRouter) {
        val controllerClass = instance::class.java

        val allMethods = ArrayList<Method>()
        getMethods(allMethods, controllerClass)
        val with = controllerClass.getAnnotation(With::class.java)?.value
        if (with != null)
            for (kClass in with) {
                getMethods(allMethods, kClass.java)
            }

        val methods = controllerClass.methods
        val befores = HashMap<Before, NewMethodInvoker>()
        val afters = HashMap<After, NewMethodInvoker>()

        for (method in allMethods) {
            val before = method.getAnnotation(Before::class.java)
            if (before != null) {
                val beforeInvoker = createMethodInvoker(instance, method)
                befores[before] = beforeInvoker
            }
            val after = method.getAnnotation(After::class.java)
            if (after != null) {
                val afterInvoker = createMethodInvoker(instance, method)
                afters[after] = afterInvoker
            }
        }

//        val before = befores.toTypedArray()
        for (method in methods) {
            val action = method.getAnnotation(Action::class.java)
            if (action != null) {
                val path: String = action.value

//                val methodInvoker = createMethodInvoker(instance, method)
                val actionInvoker = createActionInvoker(1, method, instance)
                val actionMethodName = method.name

                val abs = ArrayList<NewMethodInvoker>()
                w@ for ((before, invoker) in befores) {
                    if (before.except.size != 1 || before.except[0] != "") for (s in before.except) {
                        if (s == actionMethodName) continue@w
                    }
                    if (before.only.size != 1 || before.only[0] != "") for (s in before.only) {
                        if (s != actionMethodName) continue@w
                    }
                    abs.add(invoker)
                }
                val aas = ArrayList<NewMethodInvoker>()
                w@ for ((after, invoker) in afters) {
                    if (after.except.size != 1 || after.except[0] != "") for (s in after.except) {
                        if (s == actionMethodName) continue@w
                    }
                    if (after.only.size != 1 || after.only[0] != "") for (s in after.only) {
                        if (s != actionMethodName) continue@w
                    }
                    aas.add(invoker)
                }

                actionInvoker.befores = abs.toTypedArray()
                actionInvoker.afters = aas.toTypedArray()

//                actionInvoker.befores = before

                val tip = HashMap<Int, String>()
                val tips = method.getAnnotationsByType(ContextTip::class.java)
                        ?: method.getAnnotation(ContextTips::class.java)?.value
                if (tips != null) {
                    for (contextTip in tips) {
                        tip[contextTip.status] = contextTip.value
                    }
                }

                rootRouter.routers[path] = ContextAction(actionInvoker, tip)
            }
        }
    }

}

class ContextSession(val id: String, private val saves: MutableMap<String, Any> = ConcurrentHashMap()) {

    var suspendCoroutineIt: Continuation<Message>? = null
    var context: String? = null

    operator fun get(name: String) = saves[name]
    operator fun set(name: String, value: Any) {
        saves[name] = value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContextSession

        if (id != other.id) return false
        if (saves != other.saves) return false
        if (context != other.context) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + saves.hashCode()
        result = 31 * result + (context?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ContextSession(id='$id', saves=$saves, context=$context)"
    }

    /***
     * 挂起当前线程并等待下一条消息。
     * @param maxTime 最大等待时长，单位：毫秒。默认为 30 秒。
     * @return 下一条消息。
     * @throws WaitNextMessageTimeoutException 获取超时。
     */
    @JvmOverloads
    @Throws(WaitNextMessageTimeoutException::class)
    fun waitNextMessage(maxTime: Long = 30000): Message =
            runBlocking {
                val message = suspendCoroutine<Message> {
                    suspendCoroutineIt = it
                    GlobalScope.launch {
                        delay(maxTime)
                        if (suspendCoroutineIt != null && suspendCoroutineIt === it) {
                            suspendCoroutineIt = null
                            it.resumeWithException(WaitNextMessageTimeoutException())
                        }
                    }
                }
                suspendCoroutineIt = null
                message
            }


//    private lateinit var web: Web
//
//    suspend fun getKt(url:String) = suspendCoroutine<Message?>{
//        val client = OkHttpClient.Builder().build()
//        val req = Request.Builder().url(url).build()
//        val call = client.newCall(req)
//        call.enqueue(object : Callback{
//            override fun onFailure(call: Call, e: IOException) {
//                it.resume(null)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                TODO("Not yet implemented")
//            }
//        })
//    }

}

data class NextActionContext(val router: String, val status: Int = 0) : RuntimeException()