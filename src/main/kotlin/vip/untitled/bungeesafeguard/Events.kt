package vip.untitled.bungeesafeguard

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import vip.untitled.bungeesafeguard.config.UUIDList
import java.util.*

open class Events(val context: MetaHolderPlugin): Listener {
    protected val config: Config
        get() = context.config!!
    protected val userCache: UserCache
        get() = context.userCache!!

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
    open suspend fun updateUserCache(id: UUID, username: String) {
        val cache = userCache
        if (config.listMgr.inAnyList(id)) {
            cache.addAndSave(id, username)
        } else if (cache.contains(id)) {
            // Somehow this user is in the cache
            cache.removeAndSave(id)  // We don't need to know the username of this user anymore
        }
    }

    /**
     * Lock config and user cache, and then update user cache asynchronously
     *
     * We only care about the usernames of users in the whitelist or the blacklist
     *
     * This should be called AFTER `shouldKick`, who might update the two lists,
     * because this method will check if the user is in one of the two lists
     *
     * @param id User's UUID
     * @param username Username
     */
    open fun updateUserCacheAsync(id: UUID, username: String) = GlobalScope.launch {
        config.withLock {
            userCache.withLock {
                updateUserCache(id, username)
            }
        }
    }

    protected open fun logKick(username: String, id: UUID, kicker: UUIDList) {
        when (kicker.behavior) {
            UUIDList.Companion.Behavior.KICK_NOT_MATCHED -> context.logger.info("Player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked for not being in the ${kicker.name}")
            UUIDList.Companion.Behavior.KICK_MATCHED -> context.logger.info("Player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked for being in the ${kicker.name}")
        }
    }

    protected open fun possiblyKick(username: String, id: UUID, doKick: (UUIDList) -> Unit) {
        val config = config
        runBlocking {
            config.withLock {
                when (val kicker = config.listMgr.shouldKick(username, id)) {
                    null -> {}
                    else -> {
                        doKick(kicker)
                        logKick(username, id, kicker)
                    }
                }
            }
        }
        updateUserCacheAsync(id, username)
    }

    @EventHandler
    open fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        val username = player.name
        val id = player.uniqueId
        possiblyKick(username, id) {
            player.disconnect(TextComponent(it.message ?: ""))
        }
    }

    @EventHandler
    fun onLogin(event: LoginEvent) {
        val connection = event.connection
        val username = connection.name
        val id = connection.uniqueId
        if (id == null) {
            event.setCancelReason(TextComponent(config.noUUIDMessage ?: ""))
            event.isCancelled = true
            context.logger.info("${ChatColor.YELLOW}Player ${ChatColor.RED}${username} ${ChatColor.YELLOW}has no UUID, blocked for safety")
            return
        }
        possiblyKick(username, id) {
            event.setCancelReason(TextComponent(it.message ?: ""))
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        val player = event.player
        val username = player.name
        val id = player.uniqueId
        possiblyKick(username, id) {
            event.isCancelled = true
            player.disconnect(TextComponent(it.message ?: ""))
        }
    }
}