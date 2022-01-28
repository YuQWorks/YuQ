package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
interface YuQVersion {

    fun apiVersion() = "0.1.0.0-DEV23"
    fun runtimeName():String
    fun runtimeVersion():String

}