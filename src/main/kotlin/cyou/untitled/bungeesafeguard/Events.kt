package cyou.untitled.bungeesafeguard

import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.list.ListManager
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.LoginEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import cyou.untitled.bungeesafeguard.list.UUIDList
import kotlinx.coroutines.*
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
open class Events(val context: BungeeSafeguard): Listener {
    protected val config: Config
        get() = context.config
    protected val userCache: UserCache
        get() = context.userCache
    protected val listMgr: ListManager
        get() = context.listMgr

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
        if (listMgr.inAnyList(id)) {
            cache.addAndSave(id, username)
        } else cache.removeAndSave(id)  // We don't need to know the username of this user anymore
    }

    /**
     * Update user cache asynchronously
     *
     * We only care about the usernames of users in the whitelist or the blacklist
     *
     * This should be called AFTER `shouldKick`, who might update the two lists,
     * because this method will check if the user is in one of the two lists
     *
     * @param id User's UUID
     * @param username Username
     */
    @OptIn(DelicateCoroutinesApi::class)
    open fun updateUserCacheAsync(id: UUID, username: String) = GlobalScope.launch(context.dispatcher) {
        updateUserCache(id, username)
    }

    protected open fun logKick(username: String, id: UUID, kicker: UUIDList?) {
        when (kicker?.behavior) {
            UUIDList.Companion.Behavior.KICK_NOT_MATCHED -> context.logger.info("Player ${ChatColor.AQUA}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked for not being in the ${kicker.name}")
            UUIDList.Companion.Behavior.KICK_MATCHED -> context.logger.info("Player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} blocked for being in the ${kicker.name}")
            null -> context.logger.info("Player ${ChatColor.RED}${username} ${ChatColor.BLUE}(${id})${ChatColor.RESET} is blocked for safety because no list is enabled/loaded (yet)")
        }
    }

    protected open suspend fun possiblyKick(username: String, id: UUID, doKick: (UUIDList?) -> Unit) {
        val decision = listMgr.shouldKick(username, id)
        if (decision.kick) {
            val kicker = decision.list
            doKick(kicker)
            logKick(username, id, kicker)
        }
        updateUserCacheAsync(id, username)
    }

    @EventHandler
    open fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        val username = player.name
        val id = player.uniqueId
        runBlocking(context.dispatcher) {
            possiblyKick(username, id) {
                player.disconnect(TextComponent(it?.message ?: ""))
            }
        }
        updateUserCacheAsync(id, username)
    }

    @OptIn(DelicateCoroutinesApi::class)
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
        event.registerIntent(context)
        GlobalScope.launch(context.dispatcher) {
            possiblyKick(username, id) {
                event.setCancelReason(TextComponent(it?.message ?: ""))
                event.isCancelled = true
            }
            event.completeIntent(context)
        }
    }

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        val player = event.player
        val username = player.name
        val id = player.uniqueId
        runBlocking(context.dispatcher) {
            possiblyKick(username, id) {
                event.isCancelled = true
                player.disconnect(TextComponent(it?.message ?: ""))
            }
        }
    }
}