package vip.untitled.bungeesafeguard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.*

/**
 * A map from each player's UUID to at most 10 known usernames
 */
open class UserCache(val context: Plugin): Map<UUID, List<String>> {
    companion object {
        const val CACHE_FILE = "usercache.yml"
        const val CACHE = "cache"  // In case we want to add new entries in the future
        const val MAX_KNOWN_NAMES = 10
    }

    protected open val mutex = Mutex()

    /**
     * Lock the entire cache
     */
    open suspend fun <T> withLock(owner: Any? = null, action: suspend () -> T): T {
        mutex.withLock(owner) {
            return action()
        }
    }

    /**
     * The user cache YAML object
     */
    internal open var cache: Configuration? = null

    /**
     * The underlying map
     */
    internal open val map = mutableMapOf<UUID, MutableList<String>>()

    override val size: Int
        get() = map.size

    override fun containsKey(key: UUID): Boolean = map.containsKey(key)

    override fun containsValue(value: List<String>): Boolean = map.containsValue(value)

    override fun get(key: UUID): List<String>? = map[key]

    override fun isEmpty(): Boolean = map.isEmpty()

    override val entries: Set<Map.Entry<UUID, List<String>>>
        get() = map.entries
    override val keys: MutableSet<UUID>
        get() = map.keys
    override val values: Collection<List<String>>
        get() = map.values

    open fun clear() {
        cache?.set(CACHE, null)
        map.clear()
    }

    open suspend fun clearAndSave() {
        clear()
        save()
    }

    open fun remove(userId: UUID): List<String>? {
        cache?.set("$CACHE.$userId", null)
        return map.remove(userId)
    }

    open suspend fun removeAndSave(userId: UUID): List<String>? {
        val names = remove(userId)
        if (names != null) {
            save()
        }
        return names
    }

    /**
     * Add the username for the user ID to the cache if:
     *
     * 1. It is the first known username of the user, or
     * 2. It differs from the last known username of the user
     *
     * @return `true` if the cache is changed
     */
    open fun add(userId: UUID, username: String): Boolean {
        return if (map.contains(userId)) {
            val names = map[userId]!!
            if (names.isEmpty() || names.last() != username) {
                names.add(username)
                if (names.size > MAX_KNOWN_NAMES) {
                    names.removeFirst()
                }
                cache?.set("$CACHE.$userId", names)
                true
            } else {
                false
            }
        } else {
            val names = mutableListOf(username)
            map[userId] = names
            cache?.set("$CACHE.$userId", names)
            true
        }
    }

    open suspend fun addAndSave(userId: UUID, username: String): Boolean {
        return if (add(userId, username)) {
            save()
            true
        } else {
            false
        }
    }

    protected open val dataFolder: File
        get() = context.dataFolder

    /**
     * Create new user cache file if it does not exist yet
     */
    open suspend fun createNewCache() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        withContext(Dispatchers.IO) {
            File(dataFolder, CACHE_FILE).createNewFile()  // Create only if it does not yet exist
        }
    }

    open suspend fun reload() {
        load()
    }

    @Synchronized
    open suspend fun load() {
        val cache: Configuration
        try {
            cache = loadCacheFromFile()
            this.cache = cache
        } catch (err: IOException) {
            return
        }
        val rawCache = cache.getSection(CACHE)
        map.clear()
        for (uuidStr in rawCache.keys) {
            val uuid = try {
                UUID.fromString(uuidStr)
            } catch (err: IllegalArgumentException) {
                continue
            }
            val names = rawCache.getStringList(uuidStr)
            map[uuid] = names
        }
    }

    open suspend fun loadCacheFromFile(): Configuration {
        createNewCache()
        return withContext(Dispatchers.IO) {
            ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(File(dataFolder, CACHE_FILE))
        }
    }

    open suspend fun save(): Boolean {
        if (cache == null) {
            context.logger.warning("Cannot save user cache because it was never successfully loaded")
            return false
        }
        withContext(Dispatchers.IO) {
            ConfigurationProvider.getProvider(YamlConfiguration::class.java).save(cache, File(dataFolder, CACHE_FILE))
        }
        return true
    }
}