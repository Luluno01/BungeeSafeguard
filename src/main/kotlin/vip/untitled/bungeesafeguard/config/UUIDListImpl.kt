package vip.untitled.bungeesafeguard.config

import java.util.*

open class UUIDListImpl(
    override val name: String,
    override val lazyName: String,
    override val list: MutableSet<UUID>,
    override val lazyList: MutableSet<String>,
    @Volatile
    override var message: String?,
    override val behavior: UUIDList.Companion.Behavior,
    @Volatile
    override var enabled: Boolean
): UUIDList {
    override fun moveToListIfInLazyList(username: String, uuid: UUID): Boolean {
        return if (lazyList.contains(username)) {
            list.add(uuid)
            lazyList.remove(username)
            true
        } else {
            false
        }
    }

    override fun update(list: Set<UUID>, lazyList: Set<String>, message: String?, enabled: Boolean) {
        val mList = this.list
        val mLazyList = this.lazyList
        if (mList !== list) {
            mList.clear()
            mList.addAll(list)
        }
        if (mLazyList !== lazyList) {
            mLazyList.clear()
            mLazyList.addAll(lazyList)
        }
        this.message = message
        this.enabled = enabled
    }

    override fun inList(id: UUID): Boolean = list.contains(id)

    override fun inLazyList(username: String): Boolean = lazyList.contains(username)

    override fun addToList(record: UUID): Boolean = list.add(record)

    override fun addToLazyList(record: String): Boolean = lazyList.add(record)

    override fun removeFromList(record: UUID): Boolean = list.remove(record)

    override fun removeFromLazyList(record: String): Boolean = lazyList.remove(record)

    override fun on(): Boolean {
        return if (enabled) {
            false
        } else {
            enabled = true
            true
        }
    }

    override fun off(): Boolean {
        return if (enabled) {
            enabled = false
            true
        } else {
            false
        }
    }
}