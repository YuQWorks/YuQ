package yuq.contact

import yuq.message.items.At


/*** 群成员对象
 * 该对象描述一个群成员。
 * 群成员对象每个 Bot 每个群唯一。
 *
 * @see AnonymousMember 匿名成员
 */
interface GroupMember : Contact {

    // 该成员所属的群
    val group: Group

    /*** 群成员权限
     * 0: 普通成员
     * 1: 管理员
     * 2: 群主
     */
    val permission: Int

    /*** 群成员群名片
     * 当群成员没有设置群名片时，该值为空字符串。
     * 向参数内写入值，会实时同步修改群名片。
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    var namecard: String

    /*** 群成员群头衔
     * 当群成员没有设置群头衔时，该值为空字符串。
     * 向参数内写入值，会实时同步修改群头衔。
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    var title: String

    /*** 群成员禁言时间
     * 该值为禁言到期时间戳，单位毫秒。
     */
    val ban: Long

    // 群成员最后发言时间，单位毫秒
    val lastMessageTime: Long

    // 该成员是否被禁言
    fun isBan() = ban > (System.currentTimeMillis() / 1000).toInt()

    /*** 禁言该成员，单位秒
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    fun ban(time: Int)

    /*** 取消禁言该成员
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    fun unBan()

    // 在群名片为空时返回昵称，否则返回群名片
    fun nameCardOrName() = if (namecard == "") nickname else namecard

    // 返回一个 @群成员 的消息内容
    fun at(): At = At(id)

    /*** 该成员是否具有管理员权限
     * 该成员是管理员，或是群主都会返回 true。
     * 如果需要精确判断，请判断 [permission] 值。
     */
    fun isAdmin() = permission > 0

    // 该成员是否为群主
    fun isOwner() = permission == 2

    /*** 移除群成员
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    fun kick() = kick("")

    /*** 移除群成员
     * @param message 移除成员时的提示消息
     * @throws [PermissionDeniedException] 当权限不足时抛出
     */
    fun kick(message: String = "")

    val logStringSingle: String

//    companion object {
//        @JvmStatic
//        fun GroupMember.toFriend(): Friend? = bot.friends[id]
//    }

}