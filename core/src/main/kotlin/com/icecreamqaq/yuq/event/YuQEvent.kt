package com.icecreamqaq.yuq.event

import com.IceCreamQAQ.Yu.event.events.Event

open class YuQApplicationStatusChanged : Event(){
    open class Started : Event()
    open class Stopping : Event()
}