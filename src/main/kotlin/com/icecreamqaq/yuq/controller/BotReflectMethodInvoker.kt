package com.icecreamqaq.yuq.controller

import com.IceCreamQAQ.Yu.annotation.Action
import com.IceCreamQAQ.Yu.controller.ActionContext
import com.IceCreamQAQ.Yu.controller.MethodInvoker
import com.icecreamqaq.yuq.annotation.PathVar
import com.icecreamqaq.yuq.annotation.Save
import com.icecreamqaq.yuq.entity.*
import com.icecreamqaq.yuq.entity.Member.Companion.toFriend
import com.icecreamqaq.yuq.message.MessageItem
import com.icecreamqaq.yuq.yuq
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Named
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

class BotReflectMethodInvoker @JvmOverloads constructor(private val method: Method, val instance: Any?, level: Int? = null) : MethodInvoker {

    private var returnFlag: Boolean = false
    private var mps: Array<MethodPara?>? = null
    private val saves: Array<Saves>
    lateinit var kFun: KFunction<*>
    var isSuspend = false

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

//            if (mps.last().)

            method.kotlinFunction?.let {
                kFun = it
                if (it.isSuspend) isSuspend = true
            }

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
            type == PathVar.Type.Member -> (context.source as Group)[message.path[num].convertByPathVar(PathVar.Type.Long) as Long]
            else -> context.message.path[num].convertByPathVar(type)
        }
    }


    fun getParas(len: Int, context: BotActionContext): Array<Any?> {
        if (mps == null) return arrayOfNulls<Any>(0)
        val paras = arrayOfNulls<Any>(len)
        for (i in 0 until len) {
            val mp = mps!![i] ?: continue
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
        return paras
    }

    override suspend fun invoke(context: ActionContext): Any? {
        if (context !is BotActionContext) return null
        val mps = mps!!
        val paras = if (isSuspend) getParas(mps.size - 1, context) else getParas(mps.size, context)

        try {

            val re = if (isSuspend) kFun.callSuspend(instance, *paras)
            else if (mps.isEmpty()) method.invoke(instance)
            else method.invoke(instance, *paras)



            for (save in saves) {
                context.session[save.name] = paras[save.i] ?: continue
            }

            if (returnFlag) return re
            return null

        } catch (e: InvocationTargetException) {
            throw e.targetException!!
        }
    }

    data class MethodPara(
            val clazz: Class<*>,
            val type: Int,
            val data: Any
    )

    data class Saves(val i: Int, val name: String)
}
