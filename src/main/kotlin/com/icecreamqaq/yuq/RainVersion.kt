package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
interface RainVersion {

    fun apiVersion() = "0.1.0.0-DEV1"
    fun runtimeName():String
    fun runtimeVersion():String

}