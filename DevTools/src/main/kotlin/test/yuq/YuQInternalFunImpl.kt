package test.yuq

import com.icecreamqaq.yuq.entity.Member
import com.icecreamqaq.yuq.util.YuQInternalFun
import test.yuq.contcat.GroupMemberImpl

class YuQInternalFunImpl: YuQInternalFun() {

    override fun setMemberLastMessageTime(member: Member, time: Long) {
        (member as GroupMemberImpl).lastMessageTime = time
    }

}