package com.icecreamqaq.yuq.annotation

import com.IceCreamQAQ.Yu.annotation.EnchantBy
import com.IceCreamQAQ.Yu.annotation.LoadBy
import com.IceCreamQAQ.Yu.loader.enchant.MethodParaNamedEnchanter
import com.icecreamqaq.yuq.controller.BotContextControllerLoader
import com.icecreamqaq.yuq.controller.BotControllerLoader

import java.lang.annotation.Repeatable
import javax.inject.Named


@LoadBy(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
@Named("group")
annotation class GroupController

@LoadBy(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
@Named("priv")
annotation class PrivateController

@LoadBy(BotContextControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
annotation class ContextController
annotation class ContextAction(val value: String)

annotation class ContextTips(val value: Array<ContextTip>)

@Repeatable(ContextTips::class)
annotation class ContextTip(val value: String, val status: Int = 0)
annotation class NextContext(val value: String, val status: Int = 0)
annotation class Save(val value: String = "")

annotation class QMsg(val at: Boolean = false, val reply: Boolean = false)

annotation class PathVar(val value: Int, val type: Type = Type.String) {
    enum class Type {
        Source, String, Integer, Switch, Long, Double,Contact, Friend, Group, Member,User
    }
}