package yuq.contact

/*** 账号列表
 * 一般用于好友列表，群列表，及群成员列表等情况。
 * 虽然名称是一个 List，但他并不是一个按成员下标维护的 List！
 */
interface AccountList<P, E : Account> : Map<String, E> {

    val platformIds: Set<P>
    val platformEntries: Set<Map.Entry<P, E>>

    operator fun get(platformId: P): E?
    fun containsKey(platformId: P): Boolean

    fun getOrDefault(platformId: String, defaultValue: E): E

}