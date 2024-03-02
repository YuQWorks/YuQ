package com.icecreamqaq.yuq

import org.slf4j.LoggerFactory
import rain.application.FullStackApplicationLauncher

class YuQStarter {


    companion object {
        private val log = LoggerFactory.getLogger(YuQStarter::class.java)

        @JvmStatic
        fun start() {
            val startTime = System.currentTimeMillis()

            FullStackApplicationLauncher.launch()

            val overTime = System.currentTimeMillis()

            log.info("Done! ${(overTime - startTime).toDouble() / 1000}s.")

            println(" __  __     ____ \n" +
                    " \\ \\/ /_ __/ __ \\\n" +
                    "  \\  / // / /_/ /\n" +
                    "  /_/\\_,_/\\___\\_\\\n")
            println("感谢您使用 YuQ 进行开发，在您使用中如果遇到任何问题，可以到 Github，Gitee 提出 issue，您也可以添加 YuQ 的开发交流群（787049553）进行交流。")
        }

    }

}