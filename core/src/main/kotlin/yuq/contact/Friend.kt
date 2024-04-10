package yuq.contact

/*** 好友
 * 该对象描述一个好友。
 * 好友每个 Bot 唯一。
 */
interface Friend : Contact {

    // 删除好友
    fun delete()

}