package test.yuq

import com.IceCreamQAQ.Yu.di.YuContext
import com.IceCreamQAQ.Yu.loader.Module
import com.icecreamqaq.yuq.util.YuQInternalFun
import javax.inject.Inject

class YuQDevToolsModule : Module {

    @Inject
    private lateinit var context: YuContext

    override fun onLoad() {
        context.putBean(YuQInternalFun::class.java, "", YuQInternalFunImpl())
    }
}