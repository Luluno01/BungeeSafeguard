package vip.untitled.bungeesafeguard.config

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import java.util.*

/**
 * The list manager, owned by the config object
 * (which means you must lock the config to access the lists)
 */
open class ListManager(val context: MetaHolderPlugin) {
    /**
     * The blacklist and the whitelist
     *
     * Smaller index means higher priority
     */
    protected open val mLists: MutableList<UUIDList> = mutableListOf()
    open val lists: List<UUIDList>
        get() = mLists

    /**
     * Load a list **without locking**, create it if it does not exist
     *
     * Note that the list is identified by the unique `name`
     *
     * Once loaded, the list cannot be removed
     *
     * @param name List name
     * @param lazyList Lazy list name
     * @param list Main list
     * @param lazyName Lazy list
     * @param message Message to be sent to player if they are blocked by the list
     * @param action The action to be taken if there is a match
     * @param enabled The enabled state of the list
     */
    open fun load(
        name: String,
        lazyName: String,
        list: MutableSet<UUID>,
        lazyList: MutableSet<String>,
        message: String?,
        action: UUIDList.Companion.Behavior,
        enabled: Boolean
    ) {
        val mList = forName(name)
        mList?.update(list, lazyList, message, enabled)
            ?: mLists.add(
                UUIDListImpl(
                    name, lazyName,
                    list, lazyList,
                    message,
                    action,
                    enabled
                )
            )
    }

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

    open fun inListsWithHigherPriority(id: UUID, refListIndex: Int): List<UUIDList> {
        val higher = mutableListOf<UUIDList>()
        for (i in 0 until refListIndex) {
            val list = mLists[i]
            if (list.inList(id)) {
                higher.add(list)
            }
        }
        return higher
    }

    open fun inListsWithHigherPriority(id: UUID, refList: UUIDList): List<UUIDList> {
        return inListsWithHigherPriority(id, indexOf(refList).also { assert(it >= 0) })
    }

    open fun inListsWithLowerPriority(id: UUID, refListIndex: Int): List<UUIDList> {
        val lower = mutableListOf<UUIDList>()
        for (i in refListIndex + 1 until mLists.size) {
            val list = mLists[i]
            if (list.inList(id)) {
                lower.add(list)
            }
        }
        return lower
    }

    open fun inListsWithLowerPriority(id: UUID, refList: UUIDList): List<UUIDList> {
        return inListsWithLowerPriority(id, indexOf(refList).also { assert(it >= 0) })
    }

    open fun inLazyListsWithHigherPriority(username: String, refListIndex: Int): List<UUIDList> {
        val higher = mutableListOf<UUIDList>()
        for (i in 0 until refListIndex) {
            val list = mLists[i]
            if (list.inLazyList(username)) {
                higher.add(list)
            }
        }
        return higher
    }

    open fun inLazyListsWithHigherPriority(username: String, refList: UUIDList): List<UUIDList> {
        return inLazyListsWithHigherPriority(username, indexOf(refList).also { assert(it >= 0) })
    }

    open fun inLazyListsWithLowerPriority(username: String, refListIndex: Int): List<UUIDList> {
        val lower = mutableListOf<UUIDList>()
        for (i in refListIndex + 1 until mLists.size) {
            val list = mLists[i]
            if (list.inLazyList(username)) {
                lower.add(list)
            }
        }
        return lower
    }

    open fun inLazyListsWithLowerPriority(username: String, refList: UUIDList): List<UUIDList> {
        return inLazyListsWithLowerPriority(username, indexOf(refList).also { assert(it >= 0) })
    }

    open fun inAnyList(id: UUID): Boolean {
        for (list in lists) {
            if (list.inList(id)) return true
        }
        return false
    }

    /**
     * Check if we should kick a player
     *
     * Note this method will possibly start a background task to save the config
     *
     * @param username Player's username
     * @param id Player's UUID
     * @return The list that decides to kick the player if any
     */
    open fun shouldKick(username: String, id: UUID): UUIDList? {
        var shouldSaveConfig = false
        var kicker: UUIDList? = null
        for (list in lists) {
            if (list.moveToListIfInLazyList(username, id)) {
                shouldSaveConfig = true
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
        if (shouldSaveConfig) {
            GlobalScope.launch {
                val config = context.config!!
                config.save()
            }
        }
        for (list in lists) {
            if (list.enabled) {
                when (list.behavior) {
                    UUIDList.Companion.Behavior.KICK_NOT_MATCHED -> {
                        if (!list.inList(id)) {
                            kicker = list
                            break
                        }
                    }
                    UUIDList.Companion.Behavior.KICK_MATCHED -> {
                        if (list.inList(id)) {
                            kicker = list
                            break
                        }
                    }
                }
            }
        }
        return kicker
    }
}