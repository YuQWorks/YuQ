package yuq.test

import com.IceCreamQAQ.Yu.annotation.Action
import com.icecreamqaq.yuq.annotation.PrivateController
import kotlinx.coroutines.delay

@PrivateController
class TestController {

    @Action("HelloSuspend")
    suspend fun helloSuspend(qq: Long, spx: Int = 3): String {
        delay(5000)
        return "Hello Suspend! $qq! $spx"
    }

    @Action("HelloKotlin")
    fun helloKotlin(qq: Long, spx: Int = 3): String {
        return "Hello Kotlin! $qq! $spx"
    }

}