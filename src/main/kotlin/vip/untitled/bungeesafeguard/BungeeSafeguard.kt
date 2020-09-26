package vip.untitled.bungeesafeguard

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import vip.untitled.bungeesafeguard.commands.Blacklist
import vip.untitled.bungeesafeguard.commands.BungeeSafeguard
import vip.untitled.bungeesafeguard.commands.Whitelist
import java.io.IOException


@Suppress("unused")
open class BungeeSafeguard: ConfigHolderPlugin() {
    internal lateinit var events: Events
    private lateinit var whitelistCommand: Whitelist
    private lateinit var blacklistCommand: Blacklist
    override fun onEnable() {
        logger.info("${ChatColor.GREEN}BungeeSafeguard enabled")
        events = Events(this)
        proxy.pluginManager.registerListener(this, events)

        loadConfig(null)
        whitelistCommand = Whitelist(this)
        proxy.pluginManager.registerCommand(this, whitelistCommand)
        blacklistCommand = Blacklist(this)
        proxy.pluginManager.registerCommand(this, blacklistCommand)
        proxy.pluginManager.registerCommand(this, BungeeSafeguard(this))
    }

    override fun onDisable() {
        whitelistCommand.destroy()
        blacklistCommand.destroy()
        proxy.pluginManager.unregisterListener(events)
        logger.info("Saving configuration")
        try {
            config.save()
            logger.info("Configuration saved")
        } catch (err: Throwable) {
            logger.severe("Failed to save configuration")
            err.printStackTrace()
            logger.warning("======== Start dumping name of config file in use for data recovery ========")
            logger.warning(config.configInUse)
            logger.warning("======== End dumping name of config file in use for data recovery ========")
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
            logger.warning("======== Start dumping XBL Web API URL for data recovery ========")
            logger.warning(config.xblWebAPIUrl)
            logger.warning("======== End dumping XBL Web API URL for data recovery ========")
            logger.warning("======== Start confirmation state for data recovery ========")
            logger.warning(config.confirm.toString())
            logger.warning("======== End confirmation state for data recovery ========")
        }
    }

    open fun reloadConfig(sender: CommandSender?) {
        config.reload(sender)
    }

    open fun loadConfig(sender: CommandSender?) {
        config = Config(this)
        try {
            config.load(sender)
        } catch (err: IOException) {
            logger.warning("Unable to read config file \"${config.configInUse}\", please check the config file and if the content of \"${Config.CONFIG_IN_USE}\" is correct")
        }
    }
}