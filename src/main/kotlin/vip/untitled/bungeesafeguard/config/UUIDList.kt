package vip.untitled.bungeesafeguard.config

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import vip.untitled.bungeesafeguard.helpers.RedirectedLogger
import java.util.*

interface UUIDList {
    companion object {
        /**
         * Perform sanity check on specified list **without locking**
         */
        fun <T> checkLists(context: Plugin, sender: CommandSender?, lists: List<UUIDList>, listGetter: (UUIDList) -> Set<T>, listNameGetter: (UUIDList) -> String) {
            val logger = RedirectedLogger.get(context, sender)
            val firstOccurrence = mutableMapOf<T, UUIDList>()
            for (list in lists) {
                for (record in listGetter(list)) {
                    if (firstOccurrence.contains(record)) {
                        logger.warning("${ChatColor.AQUA}$record ${ChatColor.RESET}first presented in the ${ChatColor.AQUA}${listNameGetter(firstOccurrence[record]!!)} ${ChatColor.RESET}(higher priority), and then the ${ChatColor.AQUA}${listNameGetter(list)}")
                    } else {
                        firstOccurrence[record] = list
                    }
                }
            }
        }

        /**
         * Extract a string set from the configuration at specified path
         */
        fun <T> extractList(configuration: Configuration, path: String, transformer: (List<String>) -> MutableSet<T>): MutableSet<T> {
            return if (configuration.contains(path)) {
                transformer(configuration.getStringList(path))
            } else mutableSetOf()
        }

        /**
         * Transform raw UUID string list to a UUID set
         */
        fun transformList(rawList: List<String>): MutableSet<UUID> {
            return rawList.map { UUID.fromString(it) }.toMutableSet()
        }

        /**
         * Transform raw username list to a non-blank string set
         */
        fun transformLazyList(rawList: List<String>): MutableSet<String> {
            return rawList.filterNot { it.isBlank() }.toMutableSet()
        }

        enum class Behavior {
            KICK_NOT_MATCHED,
            KICK_MATCHED
        }
    }

    val name: String
    val lazyName: String
    val list: Set<UUID>
    val lazyList: Set<String>
    val message: String?
    val behavior: Behavior
    val enabled: Boolean

    /**
     * Move record from lazy list to main list if any
     * @param username Username
     * @param uuid UUID of the player
     * @return If the player is in lazy list
     */
    fun moveToListIfInLazyList(username: String, uuid: UUID): Boolean

    /**
     * Update the list
     * @param list The new main list
     * @param lazyList The new lazy list
     * @param message The message as block reason
     * @param enabled The new list enabled state
     */
    fun update(list: Set<UUID>, lazyList: Set<String>, message: String?, enabled: Boolean)

    /**
     * Check if given ID is in the list
     * @param id UUID of the player
     * @return Whether given ID is in the main list
     */
    fun inList(id: UUID): Boolean

    /**
     * Check if given username is in the list
     * @param username Username of the player
     * @return Whether given ID is in the lazy list
     */
    fun inLazyList(username: String): Boolean

    /**
     * Add a record to the main list
     * @param record UUID of the player
     * @return `true` if the record has been added, `false` if the record is already contained in the main list
     */
    fun addToList(record: UUID): Boolean

    /**
     * Add a record to the lazy list
     * @param record Username of the player
     * @return `true` if the record has been added, `false` if the record is already contained in the lazy list
     */
    fun addToLazyList(record: String): Boolean

    /**
     * Remove a record from the main list
     * @param record UUID of the player
     * @return `true` if the record has been successfully removed; `false` if it was not present in the main list
     */
    fun removeFromList(record: UUID): Boolean

    /**
     * Remove a record from the lazy list
     * @param record Username of the player
     * @return `true` if the record has been successfully removed; `false` if it was not present in the lazy list
     */
    fun removeFromLazyList(record: String): Boolean

    /**
     * Set the enabled state to `true`
     * @return `true` if the list was disabled; `false` otherwise
     */
    fun on(): Boolean

    /**
     * Set the enabled state to `false`
     * @return `true` if the list was enabled; `false` otherwise
     */
    fun off(): Boolean
}