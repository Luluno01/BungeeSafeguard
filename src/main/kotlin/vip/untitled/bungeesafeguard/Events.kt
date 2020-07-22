package vip.untitled.bungeesafeguard

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.*

open class Events(val context: ConfigHolderPlugin): Listener {
    protected val config: Config
        get() = context.config
    protected val whitelistMessage: String?
        get() = config.whitelistMessage
    protected val blacklistMessage: String?
        get() = config.blacklistMessage
    protected val noUUIDMessage: String?
        get() = config.noUUIDMessage
    protected val enableWhitelist: Boolean
        get() = config.enableWhitelist
    protected val enableBlacklist: Boolean
        get() = config.enableBlacklist

    /**
     * Determine whether we should kick a player
     *
     * @param username Username
     * @param id User's UUID
     */
    open fun shouldKick(username: String, id: UUID): KickReason {
        var shouldSaveConfig = false
        if (config.moveToWhitelistIfInLazyWhitelist(username, id)) {
            shouldSaveConfig = true
            context.logger.info("${ChatColor.DARK_GREEN}Move player from lazy-whitelist to whitelist ${ChatColor.AQUA}(${username} => ${id})")
        }
        if (config.moveToBlacklistIfInLazyBlacklist(username, id)) {
            shouldSaveConfig = true
            context.logger.info("${ChatColor.DARK_PURPLE}Move player from lazy-blacklist to blacklist ${ChatColor.AQUA}(${username} => ${id})")
        }
        if (shouldSaveConfig) {
            context.proxy.scheduler.runAsync(context) {
                synchronized(config) {
                    config.save()
                }
            }
        }
        if (enableBlacklist && config.inBlacklist(id)) {
            return KickReason.BLACKLISTED
        }
        if (enableWhitelist && !config.inWhitelist(id)) {
            return KickReason.NOT_WHITELISTED
        }
        return KickReason.DO_NOT_KICK
    }

    @EventHandler
    open fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        val username = player.name
        val id = player.uniqueId
        when (shouldKick(username, id)) {
            KickReason.DO_NOT_KICK -> {}
            KickReason.BLACKLISTED -> {
                if (blacklistMessage == null) player.disconnect()
                else player.disconnect(TextComponent(blacklistMessage))
                context.logger.info("Banned player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked!")
            }
            KickReason.NOT_WHITELISTED -> {
                if (whitelistMessage == null) player.disconnect()
                else player.disconnect(TextComponent(whitelistMessage))
                context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked!")
            }
        }
    }

    @EventHandler
    fun onLogin(event: LoginEvent) {
        val connection = event.connection
        val username = connection.name
        val id = connection.uniqueId
        if (id == null) {
            if (noUUIDMessage != null) event.setCancelReason(TextComponent(noUUIDMessage))
            event.isCancelled = true
            context.logger.info("${ChatColor.YELLOW}Player ${ChatColor.RED}${username} ${ChatColor.YELLOW}has no UUID, blocked for safety")
            return
        }
        when (shouldKick(username, id)) {
            KickReason.DO_NOT_KICK -> {}
            KickReason.BLACKLISTED -> {
                if (blacklistMessage != null) event.setCancelReason(TextComponent(blacklistMessage))
                event.isCancelled = true
                context.logger.info("Banned player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from logging in!")
            }
            KickReason.NOT_WHITELISTED -> {
                if (whitelistMessage != null) event.setCancelReason(TextComponent(whitelistMessage))
                event.isCancelled = true
                context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from logging in!")
            }
        }
    }

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        val player = event.player
        val username = player.name
        val id = player.uniqueId
        when (shouldKick(username, id)) {
            KickReason.DO_NOT_KICK -> {}
            KickReason.BLACKLISTED -> {
                event.isCancelled = true
                if (blacklistMessage == null) player.disconnect()
                else player.disconnect(TextComponent(blacklistMessage))
                context.logger.info("Banned player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from connecting to server ${ChatColor.GREEN}${event.target.name}!")
            }
            KickReason.NOT_WHITELISTED -> {
                event.isCancelled = true
                if (whitelistMessage == null) player.disconnect()
                else player.disconnect(TextComponent(whitelistMessage))
                context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from connecting to server ${ChatColor.GREEN}${event.target.name}!")
            }
        }
    }
}