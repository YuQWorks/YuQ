package com.icecreamqaq.yuq.internal

import com.IceCreamQAQ.Yu.annotation.Config
import com.IceCreamQAQ.Yu.annotation.Cron
import com.IceCreamQAQ.Yu.annotation.JobCenter
import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.YuQVersion
import com.icecreamqaq.yuq.yuq
import javax.inject.Inject

@JobCenter
class FrameworkInfo(
    private val version: YuQVersion,
    private val web: Web,
    @Config("yu.scanPackages")
    private var scanPackages: MutableList<String>,
    @Config("yuq.framework.uploadInfo")
    private val uploadFrameworkInfo: Boolean = true
) {

    private var rc = 0
    private var sc = 0

    var cn = 0

    private var r0 = 0
    private var r1 = 0
    private var r2 = 0
    private var r3 = 0
    private var r4 = 0
    private var r5 = 0

    private var rr = ::r0

    fun receiveMessage() {
        rc++
        rr.set(rr.get() + 1)
    }

    private var s0 = 0
    private var s1 = 0
    private var s2 = 0
    private var s3 = 0
    private var s4 = 0
    private var s5 = 0

    private var ss = ::r0

    internal fun sendMessage() {
        sc++
        ss.set(ss.get() + 1)
    }

    @Cron("10s")
    internal fun changeCount() {
        if (cn == 5) cn = 0 else cn++
        rr = when (cn) {
            0 -> ::r0
            1 -> ::r1
            2 -> ::r2
            3 -> ::r3
            4 -> ::r4
            5 -> ::r5
            else -> ::r0
        }
        rr.set(0)
        ss = when (cn) {
            0 -> ::s0
            1 -> ::s1
            2 -> ::s2
            3 -> ::s3
            4 -> ::s4
            5 -> ::s5
            else -> ::s0
        }
        ss.set(0)
    }

    init {
        scanPackages = scanPackages.filter { !it.toLowerCase().startsWith("com.icecreamqaq.yu") } as MutableList<String>
    }

    val countRm get() = r0 + r1 + r2 + r3 + r4 + r5
    val countSm get() = s0 + s1 + s2 + s3 + s4 + s5
    val countRa get() = rc
    val countSa get() = sc

    @Cron("1m", runWithStart = true)
    fun upInfo() {
        try {
            if (uploadFrameworkInfo)
                web.postJSON(
                    "https://yuq.icecreamapi.com/YuQ/runInfo2",
                    mapOf(
                        "uids" to yuq.bots.map { it.botId },
                        "yv" to version.apiVersion(),
                        "rt" to version.runtimeName(),
                        "rv" to version.runtimeVersion(),
                        "sp" to scanPackages,
                        "rm" to countRm,
                        "sm" to countSm,
                        "ra" to rc,
                        "sa" to sc,
                    )
                )
        } catch (_: Exception) {

        }
    }
}