package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.annotation.Action
import com.IceCreamQAQ.Yu.controller.NewActionContext
import com.IceCreamQAQ.Yu.controller.NewControllerLoader
import com.IceCreamQAQ.Yu.controller.router.NewActionInvoker
import com.IceCreamQAQ.Yu.controller.router.NewMethodInvoker
import com.IceCreamQAQ.Yu.entity.DoNone
import com.IceCreamQAQ.Yu.entity.Result
import com.icecreamqaq.yuq.annotation.NextContext
import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.annotation.QMsg
import com.icecreamqaq.yuq.annotation.Save
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.message.Message
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.toFriend
import com.icecreamqaq.yuq.yuq
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Named

class BotActionContext(
        val source: Contact,
        val sender: Contact,
        val message: Message,
        var session: ContextSession,
        override var path: Array<String> = message.toPath().toTypedArray()
) : NewActionContext {


    var reMessage: Message? = null

    var nextContext: NextActionContext? = null

    private val saved = HashMap<String, Any?>()

    init {
        saved["actionContext"] = this
        saved["context"] = this

        saved["session"] = session
        saved["contextSession"] = session

        saved["path"] = path

//        saved["messageId"] = message.id
//
//        saved["temp"] = message.temp
//        saved["qq"] = message.qq
//        saved["sender"] = message.qq
//        saved["group"] = message.group

        saved["sourceMessage"] = message.sourceMessage

        saved["reply"] = message.reply

        saved["message"] = message
    }

//    var message: Message? = null
//        set(message) {
//            field = message!!
//
//            path = message.toPath().toTypedArray()
//
//            saved["messageId"] = message.id
//
//            saved["temp"] = message.temp
//            saved["qq"] = message.qq
//            saved["sender"] = message.qq
//            saved["group"] = message.group
//
//            saved["sourceMessage"] = message.sourceMessage
//
//            saved["reply"] = message.reply
//
//            saved["message"] = message
//        }


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
                is NextActionContext -> {
                    this.nextContext = e
                    null
                }
                else -> e
            }

    override fun onSuccess(result: Any?): Any? {
        if (result == null) return null
        this.reMessage = buildResult(result)
        saved["reMessage"] = reMessage
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

class BotReflectMethodInvoker @JvmOverloads constructor(private val method: Method, val instance: Any?, level: Int? = null) : NewMethodInvoker {

    private var returnFlag: Boolean = false
    private var mps: Array<MethodPara?>? = null
    private val saves: Array<Saves>

    data class ParaItem(val value: Int, val type: PathVar.Type)

    init {
        if (instance != null) {
            returnFlag = (method.returnType?.name ?: "void") != "void"

            val paras = method.parameters!!
            val mps = arrayOfNulls<MethodPara>(paras.size)

            val saves = ArrayList<Saves>(paras.size)

            val action = method.getAnnotation(Action::class.java)
            val isAction = action != null && level != null
            val actionPaths = action?.value?.split(" ", "/")
            val needMatch = (actionPaths?.size ?: 0 > 1) && isAction
            val l = if (needMatch) level!! - actionPaths!!.size - 1 else 0

            para@ for ((i, para) in paras.withIndex()) {
//                val para = paras[i]!!
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

                if (needMatch) {
                    for ((ii, actionPath) in actionPaths!!.withIndex()) {
                        if (actionPath.startsWith("{") && actionPath.endsWith("}")) {
                            val needName = actionPath.subSequence(1, actionPath.length - 1)
                            if (name == needName) {
                                val type = toTyped(para.type)
                                mps[i] = MethodPara(para.type, 2, ParaItem(l + ii, type))
                                continue@para
                            }
                        }
                    }
//                    val actionPath = actionPaths!![i]
                }

                val pt = when (name) {
                    "qq" -> MethodPara(para.type, 11, toTyped(para.type))
                    "sender" -> MethodPara(para.type, 11, toTyped(para.type))
                    "group" -> MethodPara(para.type, 12, toTyped(para.type))
                    else -> null
                }
                if (pt != null) mps[i] = pt
                else mps[i] = MethodPara(para.type, 0, name)
            }


//            for (i in paras.indices) {
//                val para = paras[i]!!
//                val name = para.getAnnotation(Named::class.java)!!.value
//                val save = para.getAnnotation(Save::class.java)
//                if (save != null) {
//                    saves.add(Saves(i, name))
//                }
//
//                val pathVar = para.getAnnotation(PathVar::class.java)
//                if (pathVar != null) {
//                    mps[i] = MethodPara(para.type, 1, pathVar)
//                    continue
//                }
//
//                mps[i] = MethodPara(para.type, 0, name)
//            }

            this.mps = mps
            this.saves = saves.toTypedArray()
        } else {
            saves = ArrayList<Saves>().toTypedArray()
        }
    }

    fun toTyped(pt: Class<*>): PathVar.Type {
        var type: PathVar.Type? = null

        if (searchInterface(pt, MessageItem::class.java.name)) type = PathVar.Type.Source

        if (searchInterface(pt, User::class.java.name)) type = PathVar.Type.User
        if (searchInterface(pt, Contact::class.java.name)) type = PathVar.Type.Contact
        if (searchInterface(pt, Friend::class.java.name)) type = PathVar.Type.Friend
        if (searchInterface(pt, Group::class.java.name)) type = PathVar.Type.Group
        if (searchInterface(pt, Member::class.java.name)) type = PathVar.Type.Member

        if (type == null) {
            val ptn = pt.name
            type = when (ptn) {
                "int" -> PathVar.Type.Integer
                "java.lang.Integer" -> PathVar.Type.Integer

                "long" -> PathVar.Type.Long
                "java.lang.Long" -> PathVar.Type.Long

                "double" -> PathVar.Type.Double
                "java.lang.Double" -> PathVar.Type.Double

                "boolean" -> PathVar.Type.Switch
                "java.lang.Boolean" -> PathVar.Type.Switch

                else -> PathVar.Type.String
            }
        }

        return type
    }

    fun searchInterface(clazz: Class<*>, interfaceName: String): Boolean {
        if (clazz.name == interfaceName) return true
        for (i in clazz.interfaces) {
            if (i.name == interfaceName) return true
            if (searchInterface(i, interfaceName)) return true
        }
        return searchInterface(clazz.superclass ?: return false, interfaceName)
    }

    private fun getByPathVar(num: Int, type: PathVar.Type, context: BotActionContext): Any? {
        val message = context.message

        return when {
            message.path.size <= num -> null
            type == PathVar.Type.Source -> message.path[num]

            type == PathVar.Type.Friend -> yuq.friends[message.path[num].convertByPathVar(PathVar.Type.Long)]
            type == PathVar.Type.Group -> yuq.groups[message.path[num].convertByPathVar(PathVar.Type.Long)]
            type == PathVar.Type.Member -> yuq.groups[message.group!!]!![message.path[num].convertByPathVar(PathVar.Type.Long) as Long]
            else -> context.message.path[num].convertByPathVar(type)
        }
    }

    override fun invoke(context: NewActionContext): Any? {
        if (context !is BotActionContext) return null
        val mps = mps!!
        val paras = arrayOfNulls<Any>(mps.size)


        for (i in mps.indices) {
            val mp = mps[i] ?: continue
            paras[i] = when (mp.type) {
                0 -> context[mp.data as String]
                1 -> {
                    val pv = mp.data as PathVar
                    getByPathVar(pv.value, pv.type, context)
                }
                2 -> {
                    val pv = mp.data as ParaItem
                    getByPathVar(pv.value, pv.type, context)
                }

                11 -> {
                    when (mp.data as PathVar.Type) {
                        PathVar.Type.Long -> context.sender.id
                        PathVar.Type.Contact -> context.sender
                        PathVar.Type.Member -> if (context.sender is Member) context.sender else null
                        PathVar.Type.Friend ->
                            when (context.sender) {
                                is Friend -> context.sender
                                is Member -> context.sender.toFriend()
                                else -> null
                            }
                        else -> null
                    }
                }
                12 -> {
                    if (context.source is Group)
                        when (mp.data as PathVar.Type) {
                            PathVar.Type.Long -> context.source.id
                            PathVar.Type.Contact -> context.source
                            PathVar.Type.Group -> context.source
                            else -> null
                        }
                    else null
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

open class BotActionInvoker(level: Int, method: Method, instance: Any) : NewActionInvoker(level, method, instance) {

    var at: Boolean = false
    var reply: Boolean = false
    var nextContext: NextActionContext? = null

    override val invoker: NewMethodInvoker = BotReflectMethodInvoker(method, instance, level)


    override fun invoke(path: String, context: NewActionContext): Boolean {
        if (context !is BotActionContext) return false
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
            for (after in afters) {
                val o = after.invoke(context)
                if (o != null) context[toLowerCaseFirstOne(o::class.java.simpleName)] = o
            }
        } catch (e: Exception) {
            when (val r = context.onError(e)) {
                null -> {
                }
                is Message -> reMessage = r
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

open class BotControllerLoader : NewControllerLoader() {

    override val separationCharacter: Array<String> = arrayOf(" ", "/")

    override fun createMethodInvoker(obj: Any, method: Method) = BotReflectMethodInvoker(method, obj)

    override fun createActionInvoker(level: Int, actionMethod: Method, instance: Any): NewActionInvoker {
        val ai = BotActionInvoker(level, actionMethod, instance)
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