package com.icecreamqaq.yuq.job

import com.IceCreamQAQ.Yu.annotation.Cron
import com.IceCreamQAQ.Yu.annotation.JobCenter
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.RainVersion
import com.icecreamqaq.yuq.yuq
import javax.inject.Inject

@JobCenter
class RainInfo {
    @Inject
    private lateinit var version: RainVersion

    @Inject
    private lateinit var web: Web

    @Cron("10m")
    fun upInfo() {
        web.postJSON(
                "http://yuq.icecreamqaq.com/YuQ/runInfo",
                mapOf(
                        "uid" to yuq.botId,
                        "yv" to version.apiVersion(),
                        "rt" to version.runtimeName(),
                        "rv" to version.runtimeVersion()
                )
        )
    }
}