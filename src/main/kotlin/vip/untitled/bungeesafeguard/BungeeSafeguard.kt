package vip.untitled.bungeesafeguard

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import vip.untitled.bungeesafeguard.commands.BungeeSafeguard
import vip.untitled.bungeesafeguard.commands.ListCommand
import vip.untitled.bungeesafeguard.config.UUIDList
import java.io.IOException


@Suppress("unused")
open class BungeeSafeguard: MetaHolderPlugin() {
    internal lateinit var events: Events
    private lateinit var whitelistCommand: ListCommand
    private lateinit var blacklistCommand: ListCommand
    override fun onEnable() {
        // Load config and user cache
        runBlocking {
            joinAll(
                GlobalScope.launch { loadConfig(null) },
                GlobalScope.launch { loadUserCache() }
            )
        }

        // Register events
        events = Events(this)
        proxy.pluginManager.registerListener(this, events)

        // Register commands
        val config = config!!
        val client = HttpClient(CIO)
        whitelistCommand = ListCommand(
            this, client,
            config.listMgr, config.whitelist,
            "whitelist", "bungeesafeguard.whitelist",
            "wlist"
        )
        proxy.pluginManager.registerCommand(this, whitelistCommand)
        blacklistCommand = ListCommand(
            this, client,
            config.listMgr, config.blacklist,
            "blacklist", "bungeesafeguard.blacklist",
            "blist"
        )
        proxy.pluginManager.registerCommand(this, blacklistCommand)
        proxy.pluginManager.registerCommand(this, BungeeSafeguard(this))

        logger.info("${ChatColor.GREEN}BungeeSafeguard enabled")
    }

    override fun onDisable() {
        whitelistCommand.destroy()
        blacklistCommand.destroy()
        proxy.pluginManager.unregisterListener(events)
        logger.info("Saving configuration")
        val config = config
        if (config == null) {
            logger.severe("Config was not initialized in memory, skip config dumping")
            return
        }
        try {
            runBlocking { config.save() }
            logger.info("Configuration saved")
        } catch (err: Throwable) {
            logger.severe("Failed to save configuration")
            err.printStackTrace()
            val whitelist: UUIDList = config.whitelist
            val blacklist: UUIDList = config.blacklist
            logger.warning("======== Start dumping name of config file in use for data recovery ========")
            logger.warning(config.configInUse)
            logger.warning("======== End dumping name of config file in use for data recovery ========")
            logger.warning("======== Start dumping lazy-whitelist for data recovery ========")
            logger.warning(whitelist.lazyList.joinToString { '"' + it.replace("\"", "\\\"") + '"' })
            logger.warning("======== End dumping lazy-whitelist for data recovery ========")
            logger.warning("======== Start dumping lazy-blacklist for data recovery ========")
            logger.warning(blacklist.lazyList.joinToString { '"' + it.replace("\"", "\\\"") + '"' })
            logger.warning("======== End dumping lazy-blacklist for data recovery ========")
            logger.warning("======== Start dumping whitelist for data recovery ========")
            logger.warning(whitelist.list.joinToString())
            logger.warning("======== End dumping whitelist for data recovery ========")
            logger.warning("======== Start dumping blacklist for data recovery ========")
            logger.warning(blacklist.list.joinToString())
            logger.warning("======== End dumping blacklist for data recovery ========")
            logger.warning("======== Start dumping whitelist message for data recovery ========")
            logger.warning(config.whitelistMessage)
            logger.warning("======== End dumping whitelist message for data recovery ========")
            logger.warning("======== Start dumping blacklist message for data recovery ========")
            logger.warning(config.blacklistMessage)
            logger.warning("======== End dumping blacklist message for data recovery ========")
            logger.warning("======== Start dumping whitelist enable state for data recovery ========")
            logger.warning(whitelist.enabled.toString())
            logger.warning("======== End dumping whitelist enable state for data recovery ========")
            logger.warning("======== Start dumping blacklist enable state for data recovery ========")
            logger.warning(blacklist.enabled.toString())
            logger.warning("======== End dumping blacklist enable state for data recovery ========")
            logger.warning("======== Start dumping XBL Web API URL for data recovery ========")
            logger.warning(config.xblWebAPIUrl)
            logger.warning("======== End dumping XBL Web API URL for data recovery ========")
            logger.warning("======== Start confirmation state for data recovery ========")
            logger.warning(config.confirm.toString())
            logger.warning("======== End confirmation state for data recovery ========")
        }
    }

    open suspend fun reloadConfig(sender: CommandSender?) {
        config!!.reload(sender)
    }

    open suspend fun reloadUserCache() {
        userCache!!.reload()
    }

    open suspend fun loadConfig(sender: CommandSender?) {
        config = Config(this)
        assert(config == null) { "Config object is already initialized" }
        try {
            config!!.load(sender)
        } catch (err: IOException) {
            logger.warning("Unable to read config file \"${config!!.configInUse}\", please check the config file and if the content of \"${Config.CONFIG_IN_USE}\" is correct")
        }
    }

    open suspend fun loadUserCache() {
        assert(userCache == null) { "UserCache object is already initialized" }
        userCache = UserCache(this)
        try {
            userCache!!.load()
        } catch (err: IOException) {
            logger.warning("Unable to read user cache file \"${UserCache.CACHE_FILE}\", please check the user cache file or simply delete it")
        }
    }
}