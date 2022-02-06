package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
interface YuQVersion {

    fun apiVersion(): String = "0.1.0.0-DEV25"
    fun platform(): String = "qq"
    fun runtimeName(): String
    fun runtimeVersion(): String

}