package cyou.untitled.bungeesafeguard.storage

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import java.io.File
import java.util.*

/**
 * Storage backend
 *
 * * Implementation should be thread/coroutine-safe
 * * Backend should be registered with `Backend.Companion.registerBackend` to take effect
 *
 * Also note that we have the following observations:
 *
 * * Read is way more frequently than write
 */
abstract class Backend(val context: Plugin) {
    companion object {
        private var backend: Backend? = null
        private var isDefaultBackend = true
        private val lock = Mutex()
        @Volatile
        private var nextId = 0

        /**
         * Get backend in use
         */
        suspend fun getBackend(): Backend {
            lock.withLock {
                if (backend == null) throw IllegalStateException("No backend registered yet")
                return backend!!
            }
        }

        /**
         * If current backend is the default one
         */
        suspend fun isDefaultBackend(): Boolean = lock.withLock { isDefaultBackend }

        /**
         * Register backend, this should be called by the external backend (if installed)
         * to register itself and replace the default backend exactly once
         */
        suspend fun registerBackend(backend: Backend, plugin: Plugin? = null) {
            lock.withLock {
                val oldBackend = this.backend
                when {
                    oldBackend == null -> {
                        this.backend = backend
                        isDefaultBackend = true
                        // Don't perform sanity check now, the lists are not yet created
                    }
                    isDefaultBackend -> {
                        // Replace default backend
                        oldBackend.close(null)
                        this.backend = backend
                        isDefaultBackend = false
                    }
                    else -> throw IllegalStateException("A non-default backend is already set")
                }
                (plugin?.logger ?: BungeeSafeguard.getPlugin().logger).info("Using storage backend ${ChatColor.AQUA}$backend")
            }
        }
    }
    val id = nextId++

    /**
     * Do the initialization
     * (e.g., create the underlying file if it does not exist,
     * connect to the database)
     */
    abstract suspend fun init(commandSender: CommandSender?)

    /**
     * Close the backend
     * (e.g., close files, database connections)
     */
    abstract suspend fun close(commandSender: CommandSender?)

    /**
     * Reload from the backend
     */
    abstract suspend fun reload(commandSender: CommandSender?)

    /**
     * Add a record to the designated storage path
     *
     * @param path the storage path, e.g., `[ "whitelist", "main" ]` or `[ "whitelist", "lazy" ]`
     * @param rawRecord the record to add
     * @return `true` if the record is added, `false` if the record is already in the list
     */
    abstract suspend fun add(path: Array<String>, rawRecord: String): Boolean

    /**
     * Remove a record from the designated storage path
     *
     * @param path the storage path, e.g., `[ "whitelist", "main" ]` or `[ "whitelist", "lazy" ]`
     * @param rawRecord the record to remove
     * @return `true` if the record is removed, `false` if the record is not in the list in the first place
     */
    abstract suspend fun remove(path: Array<String>, rawRecord: String): Boolean

    /**
     * Check if a record is in the list at the designated storage path
     *
     * @param path the storage path, e.g., `[ "whitelist", "main" ]` or `[ "whitelist", "lazy" ]`
     * @param rawRecord the record to check
     * @return `true` if the list contains the record, `false` otherwise
     */
    abstract suspend fun has(path: Array<String>, rawRecord: String): Boolean

    /**
     * Get the size of the list at the designated storage path
     *
     * @param path the storage path, e.g., `[ "whitelist", "main" ]` or `[ "whitelist", "lazy" ]`
     */
    abstract suspend fun getSize(path: Array<String>): Int

    /**
     * Get a readonly copy of the list at the designated storage path
     *
     * @param path the storage path, e.g., `[ "whitelist", "main" ]` or `[ "whitelist", "lazy" ]`
     */
    abstract suspend fun get(path: Array<String>): Set<String>

    /**
     * Move record from lazy list to main list if any
     *
     * @param username username
     * @param id UUID of the player
     * @param mainPath storage path of the main list
     * @param lazyPath storage path of the lazy list
     * @return if the player is in lazy list
     */
    abstract suspend fun moveToListIfInLazyList(username: String, id: UUID, mainPath: Array<String>, lazyPath: Array<String>): Boolean

    /**
     * Possibly handle reloading of the main config file;
     *
     * Invoked by `config.load` with lock acquired
     *
     * Any exception will be considered as fatal
     */
    abstract suspend fun onReloadConfigFile(newConfig: File, commandSender: CommandSender?)
}