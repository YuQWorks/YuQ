package com.icecreamqaq.yuq.util

import com.icecreamqaq.yuq.entity.Member

abstract class YuQInternalFun {

    abstract fun setMemberLastMessageTime(member: Member, time: Long)

}