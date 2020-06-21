package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.controller.NewActionContext
import com.IceCreamQAQ.Yu.controller.NewControllerLoader
import com.IceCreamQAQ.Yu.controller.router.DefaultActionInvoker
import com.IceCreamQAQ.Yu.controller.router.MethodInvoker
import com.IceCreamQAQ.Yu.controller.router.NewActionInvoker
import com.IceCreamQAQ.Yu.controller.router.NewMethodInvoker
import com.IceCreamQAQ.Yu.entity.*
import com.icecreamqaq.yuq.annotation.NextContext
import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.annotation.QMsg
import com.icecreamqaq.yuq.annotation.Save
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import java.lang.Exception
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Named

class NewBotActionContext : NewActionContext {
    override lateinit var path: Array<String>
    lateinit var session: ContextSession

    var reMessage: Message? = null

    var nextContext: NextActionContext? = null
    private val saved = HashMap<String, Any?>()

    init {
        saved["actionContext"] = this
        saved["context"] = this
    }

    var message: Message? = null
        set(message) {
            field = message!!

            path = message.toPath().toTypedArray()

            saved["messageId"] = message.id

            saved["temp"] = message.temp
            saved["qq"] = message.qq
            saved["group"] = message.group

            saved["sourceMessage"] = message.sourceMessage

            saved["reply"] = message.reply

            saved["message"] = message
        }


    override fun get(name: String): Any? {
        return saved[name] ?: session[name]
    }

    override fun set(name: String, obj: Any) {
        saved[name] = obj
    }

    override fun onError(e: Throwable) =
            when (e) {
                is DoNone -> null
                is Result -> {
                    this.reMessage = buildResult(e)
                    reMessage
                }
                is NextActionContext ->{
                    this.nextContext = e
                    null
                }
                else -> e
            }

    override fun onSuccess(result: Any): Any? {
        this.reMessage = buildResult(result)
        return reMessage
    }

    private fun buildResult(obj: Any): Message? {
        return when (obj) {
            is String -> {
                val message = this.message!!.newMessage()
                message + obj
            }
            is MessageItem -> {
                val message = this.message!!.newMessage()
                val mb = message.body
                mb.add(obj)
                message
            }
            is Message -> obj
            else -> buildResult(obj.toString())
        }
    }
}

class NewBotReflectMethodInvoker(private val method: Method, private val instance: Any) : NewMethodInvoker {

    private var returnFlag: Boolean = false
    private var mps: Array<MethodPara?>? = null
    private val saves: Array<Saves>

    init {
        returnFlag = (method.returnType?.name ?: "void") != "void"

        val paras = method.parameters!!
        val mps = arrayOfNulls<MethodPara>(paras.size)

        val saves = ArrayList<Saves>(paras.size)
        for (i in paras.indices) {
            val para = paras[i]!!
            val name = para.getAnnotation(Named::class.java)!!.value
            val save = para.getAnnotation(Save::class.java)
            if (save != null) {
                saves.add(Saves(i, name))
            }

            val pathVar = para.getAnnotation(PathVar::class.java)
            if (pathVar != null) {
                mps[i] = MethodPara(para.type, 1, pathVar)
                continue
            }

            mps[i] = MethodPara(para.type, 0, name)
        }

        this.mps = mps
        this.saves = saves.toTypedArray()
    }

    override fun invoke(context: NewActionContext): Any? {
        if (context !is NewBotActionContext) return null
        val mps = mps!!
        val paras = arrayOfNulls<Any>(mps.size)

        for (i in mps.indices) {
            val mp = mps[i] ?: continue
            paras[i] = when (mp.type) {
                0 -> context[mp.data as String]
                1 -> {
                    val pv = mp.data as PathVar
                    when {
                        context.message!!.path.size <= pv.value -> null
                        pv.type == PathVar.Type.Source -> context.message!!.path[pv.value]
                        else -> context.message!!.path[pv.value].convertByPathVar(pv.type)
                    }
                }
                else -> null
            }
        }

        try {

            val re = if (mps.isEmpty()) method.invoke(instance)
            else method.invoke(instance, *paras)

            for (save in saves) {
                context.session[save.name] = paras[save.i] ?: continue
            }

            if (returnFlag) return re
            return null

        } catch (e: InvocationTargetException) {
            throw e.cause!!
        }
    }

    data class MethodPara(
            val clazz: Class<*>,
            val type: Int,
            val data: Any
    )

    data class Saves(val i: Int, val name: String)
}

open class NewBotActionInvoker(level: Int) : NewActionInvoker(level) {

    var at: Boolean = false
    var reply: Boolean = false
    var nextContext: NextActionContext? = null

    override fun invoke(path: String, context: NewActionContext): Boolean {
        if (context !is NewBotActionContext) return false
//        if (super.invoke(path, context)) return true
        var reMessage: Message? = null
        try {
            for (before in befores) {
                val o = before.invoke(context)
                if (o != null) context[toLowerCaseFirstOne(o::class.java.simpleName)] = o
            }
            val re = invoker.invoke(context)
            if (nextContext != null && context.nextContext == null) context.nextContext = nextContext
            reMessage = context.onSuccess(re ?: return true) as Message
        } catch (e: Exception) {
            when (val r = context.onError(e)){
                null -> {}
                is Message -> reMessage=r
                else -> throw r
            }
        }
        if (reMessage != null)
            if (reMessage.qq == null && reMessage.group == null) {
                val message = context.message!!

                reMessage.temp = message.temp
                reMessage.qq = message.qq
                reMessage.group = message.group
            }
//        context.result = reMessage

        if (reply) reMessage?.reply = context.message?.source
        if (at) reMessage?.at = true
        return true
    }

    private fun toLowerCaseFirstOne(s: String): String {
        return if (Character.isLowerCase(s[0])) s
        else (StringBuilder()).append(Character.toLowerCase(s[0])).append(s.substring(1)).toString();
    }
}

open class NewBotControllerLoader : NewControllerLoader() {

    override fun createMethodInvoker(obj: Any, method: Method) = NewBotReflectMethodInvoker(method, obj)

    override fun createActionInvoker(level: Int, actionMethod: Method): NewActionInvoker {
        val ai = NewBotActionInvoker(level)
        ai.nextContext = {
            val nc = actionMethod.getAnnotation(NextContext::class.java)
            if (nc == null) null
            else NextActionContext(nc.value, nc.status)
        }()
        val qq = actionMethod.getAnnotation(QMsg::class.java) ?: return ai
        ai.reply = qq.reply
        ai.at = qq.at
        return ai
    }
}