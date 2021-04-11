package vip.untitled.bungeesafeguard

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.*

open class Events(val context: MetaHolderPlugin): Listener {
    protected val config: Config
        get() = context.config
    protected val whitelistMessage: String
        get() = config.whitelistMessage ?: ""
    protected val blacklistMessage: String
        get() = config.blacklistMessage ?: ""
    protected val noUUIDMessage: String
        get() = config.noUUIDMessage ?: ""
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

    /**
     * Update user cache
     *
     * We only care about the usernames of users in the whitelist or the blacklist
     *
     * This should be called AFTER `shouldKick`, who might update the two lists,
     * because this method will check if the user is in one of the two lists
     *
     * @param id User's UUID
     * @param username Username
     */
    open fun updateUserCache(id: UUID, username: String) {
        val cache = context.userCache
        if (config.inWhitelist(id) || config.inBlacklist(id)) {
            cache.addAndSave(id, username)
        } else if (cache.contains(id)) {
            // Somehow this user is in the cache
            cache.removeAndSave(id)  // We don't need to know the username of this user anymore
        }
    }

    @EventHandler
    open fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        val username = player.name
        val id = player.uniqueId
        when (shouldKick(username, id)) {
            KickReason.DO_NOT_KICK -> {}
            KickReason.BLACKLISTED -> {
                player.disconnect(TextComponent(blacklistMessage))
                context.logger.info("Banned player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked!")
            }
            KickReason.NOT_WHITELISTED -> {
                player.disconnect(TextComponent(whitelistMessage))
                context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked!")
            }
        }
        updateUserCache(id, username)
    }

    @EventHandler
    fun onLogin(event: LoginEvent) {
        val connection = event.connection
        val username = connection.name
        val id = connection.uniqueId
        if (id == null) {
            event.setCancelReason(TextComponent(noUUIDMessage))
            event.isCancelled = true
            context.logger.info("${ChatColor.YELLOW}Player ${ChatColor.RED}${username} ${ChatColor.YELLOW}has no UUID, blocked for safety")
            return
        }
        when (shouldKick(username, id)) {
            KickReason.DO_NOT_KICK -> {}
            KickReason.BLACKLISTED -> {
                event.setCancelReason(TextComponent(blacklistMessage))
                event.isCancelled = true
                context.logger.info("Banned player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from logging in!")
            }
            KickReason.NOT_WHITELISTED -> {
                event.setCancelReason(TextComponent(whitelistMessage))
                event.isCancelled = true
                context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from logging in!")
            }
        }
        updateUserCache(id, username)
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
                player.disconnect(TextComponent(blacklistMessage))
                context.logger.info("Banned player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from connecting to server ${ChatColor.GREEN}${event.target.name}!")
            }
            KickReason.NOT_WHITELISTED -> {
                event.isCancelled = true
                player.disconnect(TextComponent(whitelistMessage))
                context.logger.info("Non-whitelisted player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked from connecting to server ${ChatColor.GREEN}${event.target.name}!")
            }
        }
        updateUserCache(id, username)
    }
}