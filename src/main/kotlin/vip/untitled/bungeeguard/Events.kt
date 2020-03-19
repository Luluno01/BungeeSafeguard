package vip.untitled.bungeeguard

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

open class Events(val context: ConfigHolderPlugin): Listener {
    protected val config: Config
        get() = context.config
    protected val message: String?
        get() = config.message
    @EventHandler
    open fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        val id = player.uniqueId
        if (config.inBlacklist(id)) {
            if (message == null) player.disconnect()
            else player.disconnect(TextComponent(message))
            context.logger.info("Banned player ${ChatColor.RED}${player.name} ${ChatColor.BLUE}(${player.uniqueId})${ChatColor.RESET} blocked!")
            return
        }
        if (!config.inWhitelist(id)) {
            if (message == null) player.disconnect()
            else player.disconnect(TextComponent(message))
            context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${player.name} ${ChatColor.BLUE}(${player.uniqueId})${ChatColor.RESET} blocked!")
        }
    }
}