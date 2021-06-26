package cyou.untitled.bungeesafeguard.list

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import java.util.*

/**
 * The list manager
 */
open class ListManager(@Suppress("MemberVisibilityCanBePrivate") val context: Plugin) {
    companion object {
        data class KickDecision(
            val kick: Boolean,
            val list: UUIDList?
        )
    }

    /**
     * The lists
     *
     * Smaller index means higher priority
     */
    protected open val mLists: MutableList<UUIDList> = mutableListOf()
    protected open val lock = Mutex()
    open val lists: List<UUIDList>
        get() = mLists

    open suspend fun <T>withLock(owner: Any? = null, action: suspend () -> T): T {
        lock.withLock(owner) {
            return action()
        }
    }

    /**
     * Create a new list (list created first has higher priority)
     */
    suspend fun createList(
        name: String, lazyName: String,
        path: Array<String>, lazyPath: Array<String>,
        behavior: UUIDList.Companion.Behavior,
        message: String?,
        initEnabled: Boolean,
        onSetEnabled: suspend (Boolean, CommandSender?) -> Unit
    ): UUIDListImpl = withLock { UUIDListImpl(
        name, lazyName,
        path, lazyPath,
        behavior,
        message,
        initEnabled,
        onSetEnabled
    ).also { mLists.add(it) } }

    /**
     * Get the list by its name
     */
    open fun forName(name: String): UUIDList? = mLists.find { it.name == name }

    open fun indexOf(name: String): Int {
        return mLists.indexOfFirst { it.name == name }
    }

    open fun indexOf(list: UUIDList): Int {
        return mLists.indexOf(list)
    }

    open suspend fun inListsWithHigherPriority(id: UUID, refListIndex: Int): List<UUIDList> {
        val higher = mutableListOf<UUIDList>()
        for (i in 0 until refListIndex) {
            val list = mLists[i]
            if (list.has(id)) {
                higher.add(list)
            }
        }
        return higher
    }

    open suspend fun inListsWithHigherPriority(id: UUID, refList: UUIDList): List<UUIDList> {
        return inListsWithHigherPriority(id, indexOf(refList).also { assert(it >= 0) { "Given reference list is not managed by this list manager" } })
    }

    open suspend fun inListsWithLowerPriority(id: UUID, refListIndex: Int): List<UUIDList> {
        val lower = mutableListOf<UUIDList>()
        for (i in refListIndex + 1 until mLists.size) {
            val list = mLists[i]
            if (list.has(id)) {
                lower.add(list)
            }
        }
        return lower
    }

    open suspend fun inListsWithLowerPriority(id: UUID, refList: UUIDList): List<UUIDList> {
        return inListsWithLowerPriority(id, indexOf(refList).also { assert(it >= 0) })
    }

    open suspend fun inLazyListsWithHigherPriority(username: String, refListIndex: Int): List<UUIDList> {
        val higher = mutableListOf<UUIDList>()
        for (i in 0 until refListIndex) {
            val list = mLists[i]
            if (list.lazyHas(username)) {
                higher.add(list)
            }
        }
        return higher
    }

    open suspend fun inLazyListsWithHigherPriority(username: String, refList: UUIDList): List<UUIDList> {
        return inLazyListsWithHigherPriority(username, indexOf(refList).also { assert(it >= 0) })
    }

    open suspend fun inLazyListsWithLowerPriority(username: String, refListIndex: Int): List<UUIDList> {
        val lower = mutableListOf<UUIDList>()
        for (i in refListIndex + 1 until mLists.size) {
            val list = mLists[i]
            if (list.lazyHas(username)) {
                lower.add(list)
            }
        }
        return lower
    }

    open suspend fun inLazyListsWithLowerPriority(username: String, refList: UUIDList): List<UUIDList> {
        return inLazyListsWithLowerPriority(username, indexOf(refList).also { assert(it >= 0) })
    }

    /**
     * Determine if given player is in any main list
     */
    open suspend fun inAnyList(id: UUID): Boolean = mLists.any { it.has(id) }

    /**
     * Check if we should kick a player
     *
     * Note this method will possibly start a background task to save the config
     *
     * @param username Player's username
     * @param id Player's UUID
     * @return The decision
     */
    open suspend fun shouldKick(username: String, id: UUID): KickDecision {
        if (withLock { lists.isEmpty() }) {
            return KickDecision(true, null)
        }
        var kicker: UUIDList? = null
        for (list in lists) {
            if (list.moveToListIfInLazyList(username, id)) {
                // Just update, don't make decision yet
                when (list.behavior) {
                    UUIDList.Companion.Behavior.KICK_NOT_MATCHED -> {
                        context.logger.info("${ChatColor.DARK_GREEN}Move player from ${list.lazyName} to ${list.name} ${ChatColor.AQUA}(${username} => ${id})")
                    }
                    UUIDList.Companion.Behavior.KICK_MATCHED -> {
                        context.logger.info("${ChatColor.DARK_PURPLE}Move player from ${list.lazyName} to ${list.name} ${ChatColor.AQUA}(${username} => ${id})")
                    }
                }
            }
        }
        for (list in lists) {
            if (list.enabled) {
                when (list.behavior) {
                    UUIDList.Companion.Behavior.KICK_NOT_MATCHED -> {
                        if (!list.has(id)) {
                            kicker = list
                            break
                        }
                    }
                    UUIDList.Companion.Behavior.KICK_MATCHED -> {
                        if (list.has(id)) {
                            kicker = list
                            break
                        }
                    }
                }
            }
        }
        return KickDecision(kicker != null, kicker)
    }
}