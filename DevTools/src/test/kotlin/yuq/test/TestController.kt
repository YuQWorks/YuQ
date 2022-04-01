package yuq.test

import com.IceCreamQAQ.Yu.annotation.Action
import com.icecreamqaq.yuq.annotation.PrivateController
import kotlinx.coroutines.delay

@PrivateController
class TestController {

    @Action("HelloWorld")
    suspend fun helloWorld(qq:Long): String {
        delay(5000)
        return "Hello $qq!$this"
    }

}