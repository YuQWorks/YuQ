package yuq.test

import com.IceCreamQAQ.Yu.annotation.Action
import com.icecreamqaq.yuq.annotation.PrivateController

@PrivateController
class TestController {

    @Action("HelloWorld")
    fun helloWorld() = "HelloWorld!"

}