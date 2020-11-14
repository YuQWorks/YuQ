package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
interface RainVersion {

    fun apiVersion() = "0.1.0.0-DEV8"
    fun runtimeName():String
    fun runtimeVersion():String

}