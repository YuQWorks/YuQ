package com.icecreamqaq.yuq.event

import rain.api.event.Event

interface YuQApplicationStatusChanged : Event {
    open class Started : YuQApplicationStatusChanged
    open class Stopping : YuQApplicationStatusChanged
}