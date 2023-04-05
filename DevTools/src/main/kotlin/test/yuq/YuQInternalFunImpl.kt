package test.yuq

import com.icecreamqaq.yuq.contact.Member
import test.yuq.contcat.GroupMemberImpl

class YuQInternalFunImpl: YuQInternalFun() {

    override fun setMemberLastMessageTime(member: Member, time: Long) {
        (member as GroupMemberImpl).lastMessageTime = time
    }

}