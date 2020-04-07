package vip.untitled.bungeeguard

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

open class Events(val context: ConfigHolderPlugin): Listener {
    protected val config: Config
        get() = context.config
    protected val whitelistMessage: String?
        get() = config.whitelistMessage
    protected val blacklistMessage: String?
        get() = config.blacklistMessage
    protected val enableWhitelist: Boolean
        get() = config.enableWhitelist
    protected val enableBlacklist: Boolean
        get() = config.enableBlacklist
    @EventHandler
    open fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        val id = player.uniqueId
        var shouldSaveConfig = false
        if (config.moveToWhitelistIfInLazyWhitelist(player.name, id)) {
            shouldSaveConfig = true
            context.logger.info("${ChatColor.DARK_GREEN}Move player from lazy-whitelist to whitelist ${ChatColor.AQUA}(${player.name} => ${player.uniqueId})")
        }
        if (config.moveToBlacklistIfInLazyBlacklist(player.name, id)) {
            shouldSaveConfig = true
            context.logger.info("${ChatColor.DARK_PURPLE}Move player from lazy-blacklist to blacklist ${ChatColor.AQUA}(${player.name} => ${player.uniqueId})")
        }
        if (shouldSaveConfig) {
            context.proxy.scheduler.runAsync(context) {
                synchronized(config) {
                    config.save()
                }
            }
        }
        if (enableBlacklist && config.inBlacklist(id)) {
            if (blacklistMessage == null) player.disconnect()
            else player.disconnect(TextComponent(blacklistMessage))
            context.logger.info("Banned player ${ChatColor.RED}${player.name} ${ChatColor.BLUE}(${player.uniqueId})${ChatColor.RESET} blocked!")
            return
        }
        if (enableWhitelist && !config.inWhitelist(id)) {
            if (whitelistMessage == null) player.disconnect()
            else player.disconnect(TextComponent(whitelistMessage))
            context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${player.name} ${ChatColor.BLUE}(${player.uniqueId})${ChatColor.RESET} blocked!")
        }
    }
}