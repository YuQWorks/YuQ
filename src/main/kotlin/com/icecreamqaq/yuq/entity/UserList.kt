package com.icecreamqaq.yuq.entity

import kotlin.collections.Map.Entry

/***
 * 注意，这虽然名字叫 List，但是他并不是一个按下标维护的 List！
 */
interface UserList<E : User> : Map<Long, E> {

    fun containsKey(platformId: String): Boolean
    operator fun get(platformId: String): E?

    fun getOrDefault(platformId: String, defaultValue: E): E

    val platformIds: Set<String>
    val platformEntries: Set<Entry<String, E>>
}

class UserListImpl<E : User> : UserList<E> {

    class ProEntry<E : User>(
        val idEntry: Entry<Long, E>,
        val platformIdEntry: Entry<String, E>,
        val user: E
    ) {
        inline val id get() = user.id
        inline val platformId get() = user.platformId
        override fun toString(): String {
            return "ProEntry(id=$id, platformId=$platformId, User=$user)"
        }
    }

    abstract inner class ProSet<R> : AbstractSet<R>() {
        override val size: Int
            get() = list.size

        abstract fun getByIndex(index: Int): R

        inner class ProIter : Iterator<R> {

            var i = 0

            override fun hasNext() = i >= size

            override fun next() = getByIndex(i)

        }

        override fun iterator() = ProIter()

    }


    private val list = ArrayList<ProEntry<E>>()

    override val size: Int
        get() = list.size

    override val entries: Set<Entry<Long, E>> = proSet { list[it].idEntry }
    override val platformEntries: Set<Entry<String, E>> = proSet { list[it].platformIdEntry }
    override val keys: Set<Long> = proSet { list[it].id }
    override val platformIds: Set<String> = proSet { list[it].platformId }
    override val values: Collection<E> = proSet { list[it].user }

    override fun get(key: Long): E? {
        for (entry in list) {
            if (entry.id == key) return entry.user
        }
        return null
    }

    override fun get(platformId: String): E? {
        for (entry in list) {
            if (entry.platformId == platformId) return entry.user
        }
        return null
    }

    override fun getOrDefault(key: Long, defaultValue: E) = this[key] ?: defaultValue

    override fun getOrDefault(platformId: String, defaultValue: E) = this[platformId] ?: defaultValue

    override fun containsKey(key: Long): Boolean {
        for (e in list) {
            if (e.id == key) return true
        }
        return false
    }

    override fun containsKey(platformId: String): Boolean {
        for (e in list) {
            if (e.platformId == platformId) return true
        }
        return false
    }

    override fun containsValue(value: E): Boolean {
        for (entry in list) {
            if (entry.user == value) return true
        }
        return false
    }

    override fun isEmpty() = size == 0

    private fun <T> proSet(body: (Int) -> T): ProSet<T> {
        return object : ProSet<T>() {
            override fun getByIndex(index: Int) = body(index)

        }
    }

}