package vip.untitled.bungeeguard

import net.md_5.bungee.api.ChatColor
import vip.untitled.bungeeguard.commands.Blacklist
import vip.untitled.bungeeguard.commands.BungeeGuard
import vip.untitled.bungeeguard.commands.Whitelist


open class BungeeGuard: ConfigHolderPlugin() {
    internal lateinit var events: Events
    override fun onEnable() {
        logger.info("${ChatColor.GREEN}BungeeGuard enabled")
        events = Events(this)
        proxy.pluginManager.registerListener(this, events)

        loadConfig()
        proxy.pluginManager.registerCommand(this, Whitelist(this))
        proxy.pluginManager.registerCommand(this, Blacklist(this))
        proxy.pluginManager.registerCommand(this, BungeeGuard(this))
    }

    override fun onDisable() {
        proxy.pluginManager.unregisterListener(events)
        logger.info("Saving configuration")
        try {
            config.save()
            logger.info("Configuration saved")
        } catch (err: Throwable) {
            logger.severe("Failed to save configuration")
            err.printStackTrace()
            logger.warning("======== Start dumping lazy-whitelist for data recovery ========")
            logger.warning(config.lazyWhitelist.joinToString { '"' + it.replace("\"", "\\\"") + '"' })
            logger.warning("======== End dumping lazy-whitelist for data recovery ========")
            logger.warning("======== Start dumping lazy-blacklist for data recovery ========")
            logger.warning(config.lazyBlacklist.joinToString { '"' + it.replace("\"", "\\\"") + '"' })
            logger.warning("======== End dumping lazy-blacklist for data recovery ========")
            logger.warning("======== Start dumping whitelist for data recovery ========")
            logger.warning(config.whitelist.joinToString())
            logger.warning("======== End dumping whitelist for data recovery ========")
            logger.warning("======== Start dumping blacklist for data recovery ========")
            logger.warning(config.blacklist.joinToString())
            logger.warning("======== End dumping blacklist for data recovery ========")
            logger.warning("======== Start dumping whitelist message for data recovery ========")
            logger.warning(config.whitelistMessage)
            logger.warning("======== End dumping whitelist message for data recovery ========")
            logger.warning("======== Start dumping blacklist message for data recovery ========")
            logger.warning(config.blacklistMessage)
            logger.warning("======== End dumping blacklist message for data recovery ========")
            logger.warning("======== Start dumping whitelist enable state for data recovery ========")
            logger.warning(config.enableWhitelist.toString())
            logger.warning("======== End dumping whitelist enable state for data recovery ========")
            logger.warning("======== Start dumping blacklist enable state for data recovery ========")
            logger.warning(config.enableBlacklist.toString())
            logger.warning("======== End dumping blacklist enable state for data recovery ========")
        }
    }

    open fun reloadConfig() {
        loadConfig()
    }

    open fun loadConfig() {
        config = Config(this)
        config.load()
    }
}