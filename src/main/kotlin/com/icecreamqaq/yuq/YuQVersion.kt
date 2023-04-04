package com.icecreamqaq.yuq

import com.IceCreamQAQ.Yu.annotation.AutoBind

@AutoBind
interface YuQVersion {

    fun apiVersion(): String = "0.2.0"
    fun platform(): String = "qq"
    fun runtimeName(): String
    fun runtimeVersion(): String

}