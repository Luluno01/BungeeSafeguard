package vip.untitled.bungeesafeguard

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import vip.untitled.bungeesafeguard.helpers.RedirectedLogger
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*

open class Config(val context: Plugin) {
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
    }

    /**
     * Name of the config file we are currently using (by default we use "config.yml")
     */
    @Volatile
    internal open var configInUse: String = DEFAULT_CONFIG

    @Volatile
    internal open lateinit var conf: Configuration

    /**
     * Whitelist (do not access this directly)
     */
    @Volatile
    internal open lateinit var whitelist: MutableSet<UUID>

    /**
     * Lazy-whitelist (do not access this directly)
     */
    @Volatile
    internal open lateinit var lazyWhitelist: MutableSet<String>

    /**
     * Blacklist (do not access this directly)
     */
    @Volatile
    internal open lateinit var blacklist: MutableSet<UUID>

    /**
     * Lazy-blacklist (do not access this directly)
     */
    @Volatile
    internal open lateinit var lazyBlacklist: MutableSet<String>

    @Volatile
    open var whitelistMessage: String? = null
    @Volatile
    open var blacklistMessage: String? = null
    @Volatile
    open var noUUIDMessage: String? = null
    @Volatile
    open var enableWhitelist = true
    @Volatile
    open var enableBlacklist = false
    @Volatile
    open var xblWebAPIUrl: String? = null
    @Volatile
    open var confirm: Boolean = false
    protected open val dataFolder: File
        get() = context.dataFolder

    open fun saveDefaultConfig() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val conf = File(dataFolder, DEFAULT_CONFIG)
        if (!conf.exists()) {
            context.getResourceAsStream(DEFAULT_CONFIG).use { `in` -> Files.copy(`in`, File(dataFolder, DEFAULT_CONFIG).toPath()) }
        }
    }

    open fun reload(sender: CommandSender?, configName: String? = null) {
        load(sender, configName)
    }

    @Synchronized
    open fun load(sender: CommandSender?, configName: String? = null) {
        saveDefaultConfig()
        configInUse = configName ?: loadConfigInUse(sender)
        val logger = RedirectedLogger.get(context, sender)
        logger.info("Using config file ${ChatColor.AQUA}$configInUse")
        conf = loadConfigFromFile(configInUse, sender)
        whitelist = extractWhitelist(conf)
        lazyWhitelist = extractLazyWhitelist(conf)
        blacklist = extractBlacklist(conf)
        lazyBlacklist = extractLazyBlacklist(conf)
        checkWhitelistAndBlacklist(sender, whitelist, blacklist)
        checkLazyWhitelistAndLazyBlacklist(sender, lazyWhitelist, lazyBlacklist)
        logger.info("${ChatColor.AQUA}${whitelist.size} ${ChatColor.GREEN}whitelist record(s) loaded")
        logger.info("${ChatColor.AQUA}${lazyWhitelist.size} ${ChatColor.GREEN}lazy-whitelist record(s) loaded")
        logger.info("${ChatColor.AQUA}${blacklist.size} ${ChatColor.GREEN}blacklist record(s) loaded")
        logger.info("${ChatColor.AQUA}${lazyBlacklist.size} ${ChatColor.GREEN}lazy-blacklist record(s) loaded")
        whitelistMessage = if (conf.contains(WHITELIST_MESSAGE)) conf.getString(WHITELIST_MESSAGE) else null
        blacklistMessage = if (conf.contains(BLACKLIST_MESSAGE)) conf.getString(BLACKLIST_MESSAGE) else null
        noUUIDMessage = if (conf.contains(NO_UUID_MESSAGE)) conf.getString(NO_UUID_MESSAGE) else null
        enableWhitelist = if (conf.contains(ENABLED_WHITELIST)) conf.getBoolean(ENABLED_WHITELIST) else true
        enableBlacklist = if (conf.contains(ENABLED_BLACKLIST)) conf.getBoolean(ENABLED_BLACKLIST) else false
        logger.info("${ChatColor.GREEN}Whitelist ${if (enableWhitelist) "ENABLED" else "${ChatColor.RED}DISABLED"}")
        logger.info("${ChatColor.GREEN}Blacklist ${if (enableBlacklist) "ENABLED" else "${ChatColor.RED}DISABLED"}")
        if (enableWhitelist == enableBlacklist) {
            if (enableWhitelist) logger.warning("Both blacklist and whitelist are enabled, blacklist will have a higher priority should a player is in both lists")
            else logger.warning("Both blacklist and whitelist are disabled, BungeeSafeguard will not block any player")
        }
        xblWebAPIUrl = if (conf.contains(XBL_WEB_API)) conf.getString(XBL_WEB_API) else null
        confirm = if (conf.contains(CONFIRM)) conf.getBoolean(CONFIRM) else false
    }

    /**
     * Load the name of the config in use
     */
    open fun loadConfigInUse(sender: CommandSender?): String {
        val logger = RedirectedLogger.get(context, sender)
        val inUseFile = File(dataFolder, CONFIG_IN_USE)
        return if (inUseFile.exists() && inUseFile.isFile) {
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

    open fun loadConfigFromFile(configName: String = DEFAULT_CONFIG, sender: CommandSender?): Configuration {
        val logger = RedirectedLogger.get(context, sender)
        val configFile = File(dataFolder, configName)
        if (configFile.createNewFile()) {
            logger.info("${ChatColor.AQUA}configName${ChatColor.RESET} does not exist, created an empty one")
        }
        return ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(File(dataFolder, configName))
    }

    @Synchronized
    open fun save() {
        conf.set(WHITELIST, whitelist.map { it.toString() }.toTypedArray())
        conf.set(LAZY_WHITELIST, lazyWhitelist.toTypedArray())
        conf.set(BLACKLIST, blacklist.map { it.toString() }.toTypedArray())
        conf.set(LAZY_BLACKLIST, lazyBlacklist.toTypedArray())
        conf.set(ENABLED_WHITELIST, enableWhitelist)
        conf.set(ENABLED_BLACKLIST, enableBlacklist)
        conf.set(CONFIRM, confirm)
        ConfigurationProvider.getProvider(YamlConfiguration::class.java).save(conf, File(dataFolder, configInUse))
    }

    @Synchronized
    open fun inWhitelist(id: UUID): Boolean {
        return whitelist.contains(id)
    }

    @Synchronized
    open fun inLazyWhitelist(username: String): Boolean {
        return lazyWhitelist.contains(username)
    }

    @Synchronized
    open fun addWhitelistRecord(record: UUID): Boolean {
        return whitelist.add(record)
    }

    @Synchronized
    open fun addLazyWhitelistRecord(record: String): Boolean {
        return lazyWhitelist.add(record)
    }

    @Synchronized
    open fun removeWhitelistRecord(record: UUID): Boolean {
        return whitelist.remove(record)
    }

    @Synchronized
    open fun removeLazyWhitelistRecord(record: String): Boolean {
        return lazyWhitelist.remove(record)
    }

    @Synchronized
    open fun inBlacklist(id: UUID): Boolean {
        return blacklist.contains(id)
    }

    @Synchronized
    open fun inLazyBlacklist(username: String): Boolean {
        return lazyBlacklist.contains(username)
    }

    @Synchronized
    open fun addBlacklistRecord(record: UUID): Boolean {
        return blacklist.add(record)
    }

    @Synchronized
    open fun addLazyBlacklistRecord(record: String): Boolean {
        return lazyBlacklist.add(record)
    }

    @Synchronized
    open fun removeBlacklistRecord(record: UUID): Boolean {
        return blacklist.remove(record)
    }

    @Synchronized
    open fun removeLazyBlacklistRecord(record: String): Boolean {
        return lazyBlacklist.remove(record)
    }

    /**
     * Move record from lazy-whitelist to whitelist if any
     * @param username Username
     * @param uuid UUID of the player
     * @return If the player is in lazy-whitelist
     */
    @Synchronized
    open fun moveToWhitelistIfInLazyWhitelist(username: String, uuid: UUID): Boolean {
        return if (inLazyWhitelist(username)) {
            removeLazyWhitelistRecord(username)
            addWhitelistRecord(uuid)
            true
        } else {
            false
        }
    }

    /**
     * Move record from lazy-blacklist to blacklist if any
     * @param username Username
     * @param uuid UUID of the player
     * @return If the player is in lazy-blacklist
     */
    @Synchronized
    open fun moveToBlacklistIfInLazyBlacklist(username: String, uuid: UUID): Boolean {
        return if (inLazyBlacklist(username)) {
            removeLazyBlacklistRecord(username)
            addBlacklistRecord(uuid)
            true
        } else {
            false
        }
    }

    @Synchronized
    open fun setWhitelistEnabled(enabled: Boolean) {
        enableWhitelist = enabled
    }

    @Synchronized
    open fun setBlacklistEnabled(enabled: Boolean) {
        enableBlacklist = enabled
    }

    open fun checkWhitelistAndBlacklist(sender: CommandSender?, whitelist: Set<UUID>, blacklist: Set<UUID>) {
        val logger = RedirectedLogger.get(context, sender)
        val both = whitelist.intersect(blacklist)
        if (both.isNotEmpty()) {
            logger.warning("The following UUID(s) present(s) in both whitelist and blacklist:")
            for (uuid in both) {
                logger.warning("  ${ChatColor.AQUA}${uuid}")
            }
            logger.warning("Note that blacklist has higher priority")
        }
    }

    open fun checkLazyWhitelistAndLazyBlacklist(sender: CommandSender?, lazyWhitelist: Set<String>, lazyBlacklist: Set<String>) {
        val logger = RedirectedLogger.get(context, sender)
        val both = lazyWhitelist.intersect(lazyBlacklist)
        if (both.isNotEmpty()) {
            logger.warning("The following username(s) present(s) in both lazy-whitelist and lazy-blacklist:")
            for (username in both) {
                logger.warning("  ${ChatColor.AQUA}${username}")
            }
            logger.warning("Note that blacklist has higher priority")
        }
    }

    open fun extractWhitelist(configuration: Configuration): MutableSet<UUID> {
        return if (configuration.contains(WHITELIST)) {
            configuration.getStringList(WHITELIST).map { UUID.fromString(it) }.toMutableSet()
        } else mutableSetOf()
    }

    open fun extractLazyWhitelist(configuration: Configuration): MutableSet<String> {
        return if (configuration.contains(LAZY_WHITELIST)) {
            return configuration.getStringList(LAZY_WHITELIST).filterNot { it.isBlank() }.toMutableSet()
        } else mutableSetOf()
    }

    open fun extractBlacklist(configuration: Configuration): MutableSet<UUID> {
        return if (configuration.contains(BLACKLIST)) {
            configuration.getStringList(BLACKLIST).map { UUID.fromString(it) }.toMutableSet()
        } else mutableSetOf()
    }

    open fun extractLazyBlacklist(configuration: Configuration): MutableSet<String> {
        return if (configuration.contains(LAZY_BLACKLIST)) {
            return configuration.getStringList(LAZY_BLACKLIST).filterNot { it.isBlank() }.toMutableSet()
        } else mutableSetOf()
    }
}