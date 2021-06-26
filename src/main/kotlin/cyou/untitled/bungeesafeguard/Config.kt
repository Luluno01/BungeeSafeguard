package cyou.untitled.bungeesafeguard

import cyou.untitled.bungeesafeguard.helpers.RedirectedLogger
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.list.UUIDListImpl
import cyou.untitled.bungeesafeguard.storage.Backend
import cyou.untitled.bungeesafeguard.storage.FileManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.file.Files

@Suppress("MemberVisibilityCanBePrivate")
open class Config(val context: Plugin) {
    companion object {
        const val CONFIG_IN_USE = "config-in-use.txt"
        const val DEFAULT_CONFIG = "config.yml"

        const val WHITELIST_MESSAGE = "whitelist-message"
        const val BLACKLIST_MESSAGE = "blacklist-message"
        const val NO_UUID_MESSAGE = "no-uuid-message"
        const val ENABLED_WHITELIST = "enable-whitelist"
        const val ENABLED_BLACKLIST = "enable-blacklist"
        const val XBL_WEB_API = "xbl-web-api"
        const val CONFIRM = "confirm"
    }

    protected val lock = Mutex()

    /**
     * Name of the config file we are currently using (by default we use "config.yml")
     */
    @Volatile
    open var configInUse: String = DEFAULT_CONFIG
        protected set

    @Volatile
    open var enableWhitelist: Boolean = true
    @Volatile
    open var enableBlacklist: Boolean = false

    @Volatile
    open var whitelistMessage: String? = null
        protected set
    @Volatile
    open var blacklistMessage: String? = null
        protected set
    @Volatile
    open var noUUIDMessage: String? = null
        protected set

    @Volatile
    open var xblWebAPIUrl: String? = null
        protected set

    @Volatile
    open var confirm: Boolean = false
        protected set

    protected open val dataFolder: File
        get() = context.dataFolder

    @Suppress("BlockingMethodInNonBlockingContext")
    open suspend fun saveDefaultConfig(name: String = DEFAULT_CONFIG) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val conf = File(dataFolder, name)
        if (!conf.exists()) {
            FileManager.withFile(conf.path, "config.saveDefaultConfig") {
                context.getResourceAsStream(DEFAULT_CONFIG).use { input -> Files.copy(input, conf.toPath()) }
            }
        }
    }

    /**
     * Lock the entire config
     */
    protected open suspend fun <T> withLock(owner: Any? = null, action: suspend () -> T): T {
        lock.withLock(owner) {
            return action()
        }
    }

    /**
     * Load the name of the config in use
     */
    protected open suspend fun loadConfigInUse(sender: CommandSender?): String {
        val logger = RedirectedLogger.get(context, sender)
        val inUseFile = File(dataFolder, CONFIG_IN_USE)
        return withContext(context.dispatcher) {
            FileManager.withFile(inUseFile.path, "config.loadConfigInUse") {
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
                    try {
                        File(dataFolder, CONFIG_IN_USE).writeText(DEFAULT_CONFIG)
                    } catch (err: IOException) {
                        logger.warning("File \"$CONFIG_IN_USE\" cannot be created!")
                    }
                    DEFAULT_CONFIG
                }
            }
        }
    }

    /**
     * Load the config
     */
    open suspend fun load(sender: CommandSender?, configName: String? = null, shouldUpdateLists: Boolean = true) {
        lock.withLock {
            configInUse = configName ?: loadConfigInUse(sender)
            assert(configInUse.endsWith(".yml")) { "Config must be a YAML file, got file name \"$configInUse\"" }
            saveDefaultConfig(configInUse)
            val logger = RedirectedLogger.get(context, sender)
            logger.info("Loading config file ${ChatColor.AQUA}$configInUse")
            val conf = loadConfigFromFile(configInUse, sender)
            whitelistMessage = if (conf.contains(WHITELIST_MESSAGE)) conf.getString(WHITELIST_MESSAGE) else null
            blacklistMessage = if (conf.contains(BLACKLIST_MESSAGE)) conf.getString(BLACKLIST_MESSAGE) else null
            noUUIDMessage = if (conf.contains(NO_UUID_MESSAGE)) conf.getString(NO_UUID_MESSAGE) else null
            enableWhitelist = if (conf.contains(ENABLED_WHITELIST)) conf.getBoolean(ENABLED_WHITELIST) else true
            enableBlacklist = if (conf.contains(ENABLED_BLACKLIST)) conf.getBoolean(ENABLED_BLACKLIST) else false
            xblWebAPIUrl = if (conf.contains(XBL_WEB_API)) conf.getString(XBL_WEB_API) else null
            confirm = if (conf.contains(CONFIRM)) conf.getBoolean(CONFIRM) else false
            if (shouldUpdateLists) {
                val bsg = BungeeSafeguard.getPlugin()
                val whitelist = bsg.whitelist as UUIDListImpl
                val blacklist = bsg.blacklist as UUIDListImpl
                whitelist.message = whitelistMessage
                whitelist.enabled = enableWhitelist
                blacklist.message = blacklistMessage
                blacklist.enabled = enableBlacklist
            }
            logger.info("Loaded from config file ${ChatColor.AQUA}$configInUse")
            val backend = try {
                Backend.getBackend()
            } catch (err: IllegalStateException) {
                // First time loading, backend is not yet registered
                return@withLock
            }
            try {
                backend.onReloadConfigFile(File(context.dataFolder, configInUse), sender)
            } catch (err: Throwable) {
                logger.warning("Backend $backend failed to handle config reloading: $err")
                throw err
            }
        }
    }

    /**
     * Reload the config
     */
    open suspend fun reload(sender: CommandSender?) = load(sender, configInUse, shouldUpdateLists = true)

    @Suppress("BlockingMethodInNonBlockingContext")
    protected open suspend fun doLoadConfigFromFile(configName: String = DEFAULT_CONFIG, configFile: File, sender: CommandSender?): Configuration {
        val logger = RedirectedLogger.get(context, sender)
        if (configFile.createNewFile()) {
            logger.info("${ChatColor.AQUA}$configName${ChatColor.RESET} does not exist, created an empty one")
        }
        return ConfigurationProvider.getProvider(YamlConfiguration::class.java)
            .load(File(dataFolder, configName))
    }

    protected open suspend fun loadConfigFromFile(configName: String = DEFAULT_CONFIG, sender: CommandSender?): Configuration {
        val configFile = File(dataFolder, configName)
        return FileManager.withFile(configFile.path, "config.loadConfigFromFile") {
            doLoadConfigFromFile(configName, configFile, sender)
        }
    }

    /**
     * Save the config to the underlying file
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    open suspend fun save(sender: CommandSender?) {
        withLock {
            val configFile = File(dataFolder, configInUse)
            FileManager.withFile(configFile.path, "config.save") {
                val conf = doLoadConfigFromFile(configInUse, configFile, sender)
                // Save only changeable entries
                conf.set(ENABLED_WHITELIST, enableWhitelist)
                conf.set(ENABLED_BLACKLIST, enableBlacklist)
                ConfigurationProvider.getProvider(YamlConfiguration::class.java)
                    .save(conf, File(dataFolder, configInUse))
            }
        }
    }
}