package yuq.message

interface MessageSource {
    val id: String

    /*** 撤回本消息
     * 请确保 Bot 拥有撤回权限。
     */
    suspend fun recall()
}