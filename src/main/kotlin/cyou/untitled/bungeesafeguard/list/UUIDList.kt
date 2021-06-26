package cyou.untitled.bungeesafeguard.list

import net.md_5.bungee.api.CommandSender
import java.util.*

interface UUIDList {
    companion object {
        enum class Behavior {
            KICK_NOT_MATCHED,
            KICK_MATCHED
        }
    }

    /**
     * Human-friendly main list name
     */
    val name: String

    /**
     * Human-friendly lazy list name
     */
    val lazyName: String

    /**
     * Main list storage path
     */
    val path: Array<String>

    /**
     * Lazy list storage path
     */
    val lazyPath: Array<String>

    /**
     * List behavior
     */
    val behavior: Behavior

    /**
     * Move record from lazy list to main list if any
     * @param username Username
     * @param id UUID of the player
     * @return If the player is in lazy list
     */
    suspend fun moveToListIfInLazyList(username: String, id: UUID): Boolean

    /**
     * Add a player to the main list
     *
     * @param id the record to add to the main list
     * @return `true` if the record is added, `false` if the record is already in the list
     */
    suspend fun add(id: UUID): Boolean

    /**
     * Remove a player from the main list
     *
     * @param id the record to remove from the main list
     * @return `true` if the record is removed, `false` if the record is not in the list in the first place
     */
    suspend fun remove(id: UUID): Boolean

    /**
     * Check if a player is in the main list
     *
     * @param id the record to check
     * @return `true` if the list contains the record, `false` otherwise
     */
    suspend fun has(id: UUID): Boolean

    /**
     * Add a player to the lazy list
     *
     * @param username the record to add to the lazy list
     * @return `true` if the record is added, `false` if the record is already in the list
     */
    suspend fun lazyAdd(username: String): Boolean

    /**
     * Remove a player from the lazy list
     *
     * @param username the record to remove from the lazy list
     * @return `true` if the record is removed, `false` if the record is not in the list in the first place
     */
    suspend fun lazyRemove(username: String): Boolean

    /**
     * Check if a player is in the lazy list
     *
     * @param username the record to check
     * @return `true` if the list contains the record, `false` otherwise
     */
    suspend fun lazyHas(username: String): Boolean

    /**
     * Get a readonly copy of the main list
     */
    suspend fun get(): Set<UUID>

    /**
     * Get a readonly copy of the lazy list
     */
    suspend fun lazyGet(): Set<String>

    /**
     * Message to be sent the blocked player
     */
    val message: String?

    /**
     * Set the enabled state to `true`
     * @return `true` if the list was disabled; `false` otherwise
     */
    suspend fun on(commandSender: CommandSender?): Boolean

    /**
     * Set the enabled state to `false`
     * @return `true` if the list was enabled; `false` otherwise
     */
    suspend fun off(commandSender: CommandSender?): Boolean

    /**
     * If this list is enabled (readonly)
     */
    val enabled: Boolean
}