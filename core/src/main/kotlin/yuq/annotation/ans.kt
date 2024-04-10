package yuq.annotation

import yuq.controller.BotControllerLoader
import yuq.controller.MessageChannel
import rain.api.annotation.LoadBy
import rain.classloader.enchant.EnchantBy
import rain.classloader.enchant.MethodParaNamedEnchanter
import javax.inject.Named


// 被此注解标记的内容仍在开发状态，相关类型以及名字可能随时变动，请自行评估使用价值。
annotation class Dev

// 被此注解标记的内容不推荐使用。
annotation class NoRecommendation

// 此注解标记的内容为 YuQ 内部实现，如果您不能完全理解，请不要轻易修改内容。
annotation class Internal

@LoadBy(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
@Named("group")
annotation class GroupController

@LoadBy(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
@Named("priv")
annotation class PrivateController

@LoadBy(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
@Named("guild")
annotation class GuildController

@Target(AnnotationTarget.CLASS)
@LoadBy(BotControllerLoader::class)
@EnchantBy(MethodParaNamedEnchanter::class)
annotation class BotController

@Target(AnnotationTarget.FUNCTION)
annotation class BotAction(
    val value: String,
    vararg val channel: MessageChannel = [MessageChannel.Friend, MessageChannel.GroupTemporary, MessageChannel.Group]
)

@Target(AnnotationTarget.FUNCTION)
annotation class GroupAction(val value: String)

@Target(AnnotationTarget.FUNCTION)
annotation class FriendAction(val value: String)

@Target(AnnotationTarget.FUNCTION)
annotation class TemporaryAction(val value: String)

@Target(AnnotationTarget.FUNCTION)
annotation class PrivateAction(val value: String)

//
//@LoadBy(BotContextControllerLoader::class)
//@EnchantBy(MethodParaNamedEnchanter::class)
//annotation class ContextController
//annotation class ContextAction(val value: String)

//annotation class ContextTips(val value: Array<ContextTip>)

annotation class RainCodeString

//@Repeatable(ContextTips::class)
//annotation class ContextTip(val value: String, val status: Int = 0)
//annotation class NextContext(val value: String, val status: Int = 0)
//annotation class Save(val value: String = "")

//annotation class QMsg(
//    val at: Boolean = false,
//    val reply: Boolean = false,
//    val atNewLine: Boolean = false,
//    val mastAtBot: Boolean = false,
//    val recall: Long = 0,
//    val forceMatch: Boolean = false,
//)
//
//annotation class PathVar(val value: Int, val type: Type = Type.String) {
//    enum class Type {
//        Source, String, Integer, Switch, Long, Double, Contact, Friend, Group, Member, User
//    }
//}
//
//annotation class AsyncAction
//annotation class TaskLimit(
//    val value: Long,
//    val type: TaskLimitSource = TaskLimitSource.SENDER,
//    val extraPermission: String = "",
//    val coldDownTip: String = "冷却中。",
//) {
//    enum class TaskLimitSource {
//        SENDER, SOURCE, ALL
//    }
//}
