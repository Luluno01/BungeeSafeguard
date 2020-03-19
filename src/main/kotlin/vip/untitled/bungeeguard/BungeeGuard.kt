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
    }

    open fun reloadConfig() {
        loadConfig()
    }

    open fun loadConfig() {
        config = Config(this)
        config.load()
    }
}