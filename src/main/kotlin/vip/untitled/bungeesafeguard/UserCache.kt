package vip.untitled.bungeesafeguard

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

    /**
     * The user cache YAML object
     */
    @Volatile
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

    @Synchronized
    open fun clear() {
        cache?.set(CACHE, null)
        map.clear()
    }

    @Synchronized
    open fun clearAndSave() {
        clear()
        save()
    }

    @Synchronized
    open fun remove(userId: UUID): List<String>? {
        cache?.set("$CACHE.$userId", null)
        return map.remove(userId)
    }

    @Synchronized
    open fun removeAndSave(userId: UUID): List<String>? {
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
    @Synchronized
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

    @Synchronized
    open fun addAndSave(userId: UUID, username: String): Boolean {
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
    open fun createNewCache() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        File(dataFolder, CACHE_FILE).createNewFile()  // Create only if it does not yet exist
    }

    open fun reload() {
        load()
    }

    @Synchronized
    open fun load() {
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

    open fun loadCacheFromFile(): Configuration {
        createNewCache()
        return ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(File(dataFolder, CACHE_FILE))
    }

    @Synchronized
    open fun save(): Boolean {
        if (cache == null) {
            context.logger.warning("Cannot save user cache because it was never successfully loaded")
            return false
        }
        ConfigurationProvider.getProvider(YamlConfiguration::class.java).save(cache, File(dataFolder, CACHE_FILE))
        return true
    }
}