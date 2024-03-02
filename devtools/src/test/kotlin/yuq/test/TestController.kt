package yuq.test

import com.icecreamqaq.yuq.annotation.BotAction
import com.icecreamqaq.yuq.annotation.BotController
import com.icecreamqaq.yuq.contact.GroupMember
import kotlinx.coroutines.delay

@BotController
class TestController {

    @BotAction("HelloSuspend")
    suspend fun helloSuspend(qq: Long, spx: Int = 3): String {
        delay(1000)
        return "Hello Suspend! $qq! $spx"
    }

    @BotAction("HelloKotlin")
    fun helloKotlin(qq: Long, spx: String?): String {
        return "Hello Kotlin! $qq! $spx"
    }

    // 可以匹配内容 你好 @甲 @乙 @丙 @丁
    @BotAction("你好 {sbs}")
    fun helloArray(sbs: Array<GroupMember>) {

    }

    /***
     * 可以将 "Map匹配体质40精神60攻击20"
     * 转化为 {
     *      "体质": 40,
     *      "精神": 60,
     *      "攻击": 20
     * }
     */
    @BotAction("Map匹配{param}")
    fun map(param: Map<String, Int>) {
        println(param)
    }

    enum class Sex {
        男, 女
    }

    /***
     * 可以将 "录入成员张三男李四女王五男"
     * 转化为 {
     *     "张三": 男,
     *     "李四": 女,
     *     "王五": 男
     * }
     */
    @BotAction("录入成员{param}")
    fun map(param: Map<String, Sex>) {
        println(param)
    }

    @BotAction("测试 [a] {b}")
    fun test(a: String, b: Int) {
        println("a: $a, b: $b")
    }

}