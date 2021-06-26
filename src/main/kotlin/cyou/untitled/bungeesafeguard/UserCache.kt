package cyou.untitled.bungeesafeguard

import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.storage.FileManager
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
 *
 * FIXME: `clear` and `remove` cannot be compiled and are temporarily commented out
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
    protected open suspend fun <T> withLock(owner: Any? = null, action: suspend () -> T): T {
        mutex.withLock(owner) {
            return action()
        }
    }

    /**
     * The user cache YAML object
     */
    protected open var cache: Configuration? = null

    /**
     * The underlying map
     */
    protected open val map = mutableMapOf<UUID, MutableList<String>>()

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

    protected open fun doClear() {
        cache?.set(CACHE, null)
        map.clear()
    }

//    open suspend fun clear() = withLock { doClear() }

    open suspend fun clearAndSave() {
        withLock {
            doClear()
            doSave()
        }
    }

    protected open fun doRemove(userId: UUID): List<String>? {
        cache?.set("$CACHE.$userId", null)
        return map.remove(userId)
    }

//    open suspend fun remove(userId: UUID): List<String>? = withLock { doRemove(userId) }

    open suspend fun removeAndSave(userId: UUID): List<String>? {
        return withLock {
            val names = doRemove(userId)
            if (names != null) {
                doSave()
            }
            return@withLock names
        }
    }

    /**
     * Add the username for the user ID to the cache if:
     *
     * 1. It is the first known username of the user, or
     * 2. It differs from the last known username of the user
     *
     * @return `true` if the cache is changed
     */
    protected open fun doAdd(userId: UUID, username: String): Boolean {
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

//    /**
//     * Add the username for the user ID to the cache if:
//     *
//     * 1. It is the first known username of the user, or
//     * 2. It differs from the last known username of the user
//     *
//     * @return `true` if the cache is changed
//     */
//    open suspend fun add(userId: UUID, username: String): Boolean {
//        return withLock { doAdd(userId, username) }
//    }

    open suspend fun addAndSave(userId: UUID, username: String): Boolean {
        return withLock {
            return@withLock if (doAdd(userId, username)) {
                doSave()
                true
            } else {
                false
            }
        }
    }

    protected open val dataFolder: File
        get() = context.dataFolder

    /**
     * Create new user cache file if it does not exist yet
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    open suspend fun createNewCache() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        withContext(context.dispatcher) {
            File(dataFolder, CACHE_FILE).createNewFile()  // Create only if it does not yet exist
        }
    }

    open suspend fun reload() {
        load()
    }

    protected open suspend fun doLoad() {
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

    open suspend fun load() {
        withLock { doLoad() }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    open suspend fun loadCacheFromFile(): Configuration {
        createNewCache()
        val cacheFile = File(dataFolder, CACHE_FILE)
        return FileManager.withFile(cacheFile.path, "userCache.loadCacheFromFile") {
            return@withFile withContext(context.dispatcher) {
                ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(File(dataFolder, CACHE_FILE))
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    protected open suspend fun doSave(): Boolean {
        if (cache == null) {
            context.logger.warning("Cannot save user cache because it was never successfully loaded")
            return false
        }
        val cacheFile = File(dataFolder, CACHE_FILE)
        FileManager.withFile(cacheFile.path, "userCache.doSave") {
            withContext(context.dispatcher) {
                ConfigurationProvider.getProvider(YamlConfiguration::class.java)
                    .save(cache, File(dataFolder, CACHE_FILE))
            }
        }
        return true
    }

    open suspend fun save(): Boolean {
        return withLock { doSave() }
    }
}