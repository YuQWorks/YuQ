package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
interface RainVersion {

    fun apiVersion() = "0.0.6.10"
    fun runtimeName():String
    fun runtimeVersion():String

}