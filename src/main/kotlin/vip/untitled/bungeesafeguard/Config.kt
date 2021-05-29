package vip.untitled.bungeesafeguard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import vip.untitled.bungeesafeguard.config.ListManager
import vip.untitled.bungeesafeguard.config.UUIDList
import vip.untitled.bungeesafeguard.config.UUIDList.Companion.checkLists
import vip.untitled.bungeesafeguard.config.UUIDList.Companion.extractList
import vip.untitled.bungeesafeguard.config.UUIDList.Companion.transformLazyList
import vip.untitled.bungeesafeguard.config.UUIDList.Companion.transformList
import vip.untitled.bungeesafeguard.helpers.RedirectedLogger
import java.io.File
import java.io.IOException
import java.nio.file.Files

open class Config(val context: MetaHolderPlugin) {
    companion object {
        const val CONFIG_IN_USE = "config-in-use.txt"
        const val DEFAULT_CONFIG = "config.yml"
        const val WHITELIST = "whitelist"
        const val LAZY_WHITELIST = "lazy-whitelist"
        const val BLACKLIST = "blacklist"
        const val LAZY_BLACKLIST = "lazy-blacklist"
        const val WHITELIST_MESSAGE = "whitelist-message"
        const val BLACKLIST_MESSAGE = "blacklist-message"
        const val NO_UUID_MESSAGE = "no-uuid-message"
        const val ENABLED_WHITELIST = "enable-whitelist"
        const val ENABLED_BLACKLIST = "enable-blacklist"
        const val XBL_WEB_API = "xbl-web-api"
        const val CONFIRM = "confirm"

        const val WHITELIST_NAME = "whitelist"
        const val LAZY_WHITELIST_NAME = "lazy-whitelist"
        const val BLACKLIST_NAME = "blacklist"
        const val LAZY_BLACKLIST_NAME = "lazy-blacklist"
    }

    protected open val mutex = Mutex()

    /**
     * Name of the config file we are currently using (by default we use "config.yml")
     */
    @Volatile
    internal open var configInUse: String = DEFAULT_CONFIG

    @Volatile
    internal open lateinit var conf: Configuration

    /**
     * The list manager
     */
    open val listMgr = ListManager(context)

    @Volatile
    open var whitelistMessage: String? = null
    @Volatile
    open var blacklistMessage: String? = null
    @Volatile
    open var noUUIDMessage: String? = null
    @Volatile
    open var xblWebAPIUrl: String? = null
    @Volatile
    open var confirm: Boolean = false
    protected open val dataFolder: File
        get() = context.dataFolder

    open suspend fun saveDefaultConfig(name: String = DEFAULT_CONFIG) {
        withContext(Dispatchers.IO) {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs()
            }
        }
        val conf = File(dataFolder, name)
        withContext(Dispatchers.IO) {
            if (!conf.exists()) {
                context.getResourceAsStream(DEFAULT_CONFIG).use { `in` -> Files.copy(`in`, conf.toPath()) }
            }
        }
    }

    /**
     * Lock the entire config
     */
    open suspend fun <T> withLock(owner: Any? = null, action: suspend () -> T): T {
        mutex.withLock(owner) {
            return action()
        }
    }

    open val whitelist: UUIDList
        get() {
            return listMgr.forName(WHITELIST_NAME) ?: throw IllegalStateException("Lists are not yet initialized")
        }

    open val blacklist: UUIDList
        get() {
            return listMgr.forName(BLACKLIST_NAME) ?: throw IllegalStateException("Lists are not yet initialized")
        }

    /**
     * Reload the config
     */
    open suspend fun reload(sender: CommandSender?, configName: String? = null) {
        load(sender, configName)
    }

    /**
     * Load the config
     */
    @Synchronized
    open suspend fun load(sender: CommandSender?, configName: String? = null) {
        configInUse = configName ?: loadConfigInUse(sender)
        assert(configInUse.endsWith(".yml")) { "Config must be a YAML file, got file name \"$configInUse\"" }
        saveDefaultConfig(configInUse)
        val logger = RedirectedLogger.get(context, sender)
        logger.info("Using config file ${ChatColor.AQUA}$configInUse")
        conf = loadConfigFromFile(configInUse, sender)
        val rawWhitelist = extractList(conf, WHITELIST) { transformList(it) }
        val lazyWhitelist = extractList(conf, LAZY_WHITELIST) { transformLazyList(it) }
        val rawBlacklist = extractList(conf, BLACKLIST) { transformList(it) }
        val lazyBlacklist = extractList(conf, LAZY_BLACKLIST) { transformLazyList(it) }
        whitelistMessage = if (conf.contains(WHITELIST_MESSAGE)) conf.getString(WHITELIST_MESSAGE) else null
        blacklistMessage = if (conf.contains(BLACKLIST_MESSAGE)) conf.getString(BLACKLIST_MESSAGE) else null
        val enableWhitelist = if (conf.contains(ENABLED_WHITELIST)) conf.getBoolean(ENABLED_WHITELIST) else true
        val enableBlacklist = if (conf.contains(ENABLED_BLACKLIST)) conf.getBoolean(ENABLED_BLACKLIST) else false
        listMgr.load(BLACKLIST_NAME, LAZY_BLACKLIST_NAME, rawBlacklist, lazyBlacklist, blacklistMessage, UUIDList.Companion.Behavior.KICK_MATCHED, enableBlacklist)
        listMgr.load(WHITELIST_NAME, LAZY_WHITELIST_NAME, rawWhitelist, lazyWhitelist, whitelistMessage, UUIDList.Companion.Behavior.KICK_NOT_MATCHED, enableWhitelist)
        val lists = listMgr.lists
        checkLists(context, sender, lists, { it.list }, { it.name })
        checkLists(context, sender, lists, { it.lazyList }, { it.lazyName })
        var enabledListsNum = 0
        for (list in lists) {
            logger.info("${ChatColor.GREEN}${list.name.capitalize()} ${if (list.enabled) "ENABLED" else "${ChatColor.RED}DISABLED"}")
            logger.info("${ChatColor.AQUA}${list.list.size} ${ChatColor.GREEN}${list.name} record(s) loaded")
            logger.info("${ChatColor.AQUA}${list.lazyList.size} ${ChatColor.GREEN}${list.lazyName} record(s) loaded")
            if (list.enabled) enabledListsNum++
        }
        when {
            enabledListsNum == 0 -> logger.warning("All lists are disabled, BungeeSafeguard will not block any player")
            enabledListsNum > 1 -> logger.warning("Multiple lists are enabled (priorities: ${lists.joinToString("> ") { it.name }})")
        }
        noUUIDMessage = if (conf.contains(NO_UUID_MESSAGE)) conf.getString(NO_UUID_MESSAGE) else null
        xblWebAPIUrl = if (conf.contains(XBL_WEB_API)) conf.getString(XBL_WEB_API) else null
        confirm = if (conf.contains(CONFIRM)) conf.getBoolean(CONFIRM) else false
    }

    /**
     * Load the name of the config in use
     */
    protected open suspend fun loadConfigInUse(sender: CommandSender?): String {
        val logger = RedirectedLogger.get(context, sender)
        val inUseFile = File(dataFolder, CONFIG_IN_USE)
        return withContext(Dispatchers.IO) {
            if (inUseFile.exists() && inUseFile.isFile) {
                try {
                    val name = inUseFile.readText().trim()
                    val confFile = File(dataFolder, name)
                    if (confFile.exists() && confFile.isFile) {
                        name
                    } else {
                        logger.warning("Specified file \"$name\" does not exist, fallback to the default config \"$DEFAULT_CONFIG\"")
                        DEFAULT_CONFIG
                    }
                } catch (err: IOException) {
                    logger.warning("Cannot read \"$CONFIG_IN_USE\", fallback to the default config \"$DEFAULT_CONFIG\"")
                    DEFAULT_CONFIG
                }
            } else {
                logger.warning("File \"$CONFIG_IN_USE\" not found, fallback to the default config \"$DEFAULT_CONFIG\"")
                DEFAULT_CONFIG
            }
        }
    }

    protected open suspend fun loadConfigFromFile(configName: String = DEFAULT_CONFIG, sender: CommandSender?): Configuration {
        val logger = RedirectedLogger.get(context, sender)
        val configFile = File(dataFolder, configName)
        return withContext(Dispatchers.IO) {
            if (configFile.createNewFile()) {
                logger.info("${ChatColor.AQUA}configName${ChatColor.RESET} does not exist, created an empty one")
            }
            ConfigurationProvider.getProvider(YamlConfiguration::class.java)
                .load(File(dataFolder, configName))
        }
    }

    /**
     * Save the config to the underlying file
     */
    @Synchronized
    open suspend fun save() {
        withLock {
            val mWhitelist = whitelist
            val mBlacklist = blacklist
            val rawWhitelist = mWhitelist.list
            val lazyWhitelist = mWhitelist.lazyList
            val enableWhitelist = mWhitelist.enabled
            val rawBlacklist = mBlacklist.list
            val lazyBlacklist = mBlacklist.lazyList
            val enableBlacklist = mBlacklist.enabled
            conf.set(WHITELIST, rawWhitelist.map { it.toString() }.toTypedArray())
            conf.set(LAZY_WHITELIST, lazyWhitelist.toTypedArray())
            conf.set(BLACKLIST, rawBlacklist.map { it.toString() }.toTypedArray())
            conf.set(LAZY_BLACKLIST, lazyBlacklist.toTypedArray())
            conf.set(ENABLED_WHITELIST, enableWhitelist)
            conf.set(ENABLED_BLACKLIST, enableBlacklist)
            conf.set(CONFIRM, confirm)
            withContext(Dispatchers.IO) {
                ConfigurationProvider.getProvider(YamlConfiguration::class.java)
                    .save(conf, File(dataFolder, configInUse))
            }
        }
    }
}