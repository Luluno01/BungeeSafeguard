package cyou.untitled.bungeesafeguard

import cyou.untitled.bungeesafeguard.commands.ListCommandImpl
import cyou.untitled.bungeesafeguard.events.BungeeSafeguardEnabledEvent
import cyou.untitled.bungeesafeguard.helpers.ListChecker
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import cyou.untitled.bungeesafeguard.storage.Backend
import cyou.untitled.bungeesafeguard.storage.CachedBackend
import cyou.untitled.bungeesafeguard.storage.ConfigBackend
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ChatColor
import org.bstats.bungeecord.Metrics
import java.io.File
import cyou.untitled.bungeesafeguard.commands.BungeeSafeguard as BSGCmd

class BungeeSafeguardImpl: BungeeSafeguard() {
    override val config = Config(this)
    override val userCache = UserCache(this)
    override val listMgr = ListManager(this)
    private val events = Events(this)
    override lateinit var whitelist: UUIDList
    override lateinit var blacklist: UUIDList
    override lateinit var whitelistCommand: ListCommandImpl
    override lateinit var blacklistCommand: ListCommandImpl
    override var enabled: Boolean = false
        private set

    override fun onEnable() {
        runBlocking(dispatcher) {
            lateinit var defaultBackend: Backend
            joinAll(
                launch {
                    config.load(null, shouldUpdateLists = false)

                    // Init lists which depend on config
                    // Blacklist has higher priority
                    blacklist = listMgr.createList(
                        BLACKLIST_NAME, LAZY_BLACKLIST_NAME,
                        BLACKLIST, LAZY_BLACKLIST,
                        UUIDList.Companion.Behavior.KICK_MATCHED,
                        config.blacklistMessage,
                        config.enableBlacklist
                    ) { enabled, commandSender ->
                        config.enableBlacklist = enabled
                        config.save(commandSender)
                    }

                    // Whitelist has lower priority
                    whitelist = listMgr.createList(
                        WHITELIST_NAME, LAZY_WHITELIST_NAME,
                        WHITELIST, LAZY_WHITELIST,
                        UUIDList.Companion.Behavior.KICK_NOT_MATCHED,
                        config.whitelistMessage,
                        config.enableWhitelist
                    ) { enabled, commandSender ->
                        config.enableWhitelist = enabled
                        config.save(commandSender)
                    }

                    // Init backend which depends on config and the lists
                    defaultBackend = CachedBackend(
                        this@BungeeSafeguardImpl,
                        ConfigBackend(this@BungeeSafeguardImpl, File(dataFolder, config.configInUse)),
                        arrayOf(BLACKLIST, LAZY_BLACKLIST, WHITELIST, LAZY_WHITELIST)
                    )
                    defaultBackend.init(null)  // Default backend must be loaded after the config because otherwise the config may not be created yet
                    ListChecker.checkLists(this@BungeeSafeguardImpl, null, listMgr, { defaultBackend.get(it.lazyPath) }, { it.lazyName })
                    ListChecker.checkLists(this@BungeeSafeguardImpl, null, listMgr, { defaultBackend.get(it.path) }, { it.name })
                    Backend.registerBackend(defaultBackend, this@BungeeSafeguardImpl)
                },
                launch { userCache.load() }  // User cache does not depend on config or the default backend
            )
        }

        // Register events
        proxy.pluginManager.registerListener(this, events)

        proxy.pluginManager.registerCommand(this, BSGCmd(this))
        whitelistCommand = ListCommandImpl(
            this, listMgr,
            whitelist, "whitelist",
            "bungeesafeguard.whitelist", "wlist"
        )
        proxy.pluginManager.registerCommand(this, whitelistCommand)
        blacklistCommand = ListCommandImpl(
            this, listMgr,
            blacklist, "blacklist",
            "bungeesafeguard.blacklist", "blist"
        )
        proxy.pluginManager.registerCommand(this, blacklistCommand)

        Metrics(this, 11845)

        exposeInst()
        logger.info("${ChatColor.GREEN}BungeeSafeguard enabled")
        enabled = true
        proxy.pluginManager.callEvent(BungeeSafeguardEnabledEvent(this))
    }

    override fun onDisable() {
        whitelistCommand.destroy()
        blacklistCommand.destroy()
        proxy.pluginManager.unregisterCommands(this)
        proxy.pluginManager.unregisterListener(events)
        logger.info("Saving configuration")
        try {
            runBlocking /* No more asynchronous tasks will be executed */ { config.save(null) }
            logger.info("Configuration saved")
        } catch (err: Throwable) {
            logger.severe("Failed to save configuration")
            err.printStackTrace()
            logger.warning("======== Start dumping name of config file in use for data recovery ========")
            logger.warning(config.configInUse)
            logger.warning("======== End dumping name of config file in use for data recovery ========")
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
        runBlocking /* No more asynchronous tasks will be executed */ {
            val backend = Backend.getBackend()
            val backendDesc = backend.toString()
            logger.info("Closing backend $backendDesc")
            backend.close(null)
            logger.info("Backend $backendDesc closed")
        }
    }
}