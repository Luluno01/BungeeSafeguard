package cyou.untitled.bungeesafeguard

import cyou.untitled.bungeesafeguard.commands.ListCommand
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import net.md_5.bungee.api.plugin.Plugin

/**
 * Abstract BungeeSafeguard plugin as the interface exposed
 *
 * Third party can get access to BungeeSafeguard's API via `BungeeSafeguard.Companion.getPlugin`
 */
abstract class BungeeSafeguard: Plugin() {
    companion object {
        @Volatile
        private lateinit var inst: BungeeSafeguard

        /**
         * Get the instance of `BungeeSafeguard`
         */
        fun getPlugin(): BungeeSafeguard {
            return inst
        }

        val WHITELIST = arrayOf("whitelist", "main")
        val LAZY_WHITELIST = arrayOf("whitelist", "lazy")
        const val WHITELIST_NAME = "whitelist"
        const val LAZY_WHITELIST_NAME = "lazy-whitelist"
        val BLACKLIST = arrayOf("blacklist", "main")
        val LAZY_BLACKLIST = arrayOf("blacklist", "lazy")
        const val BLACKLIST_NAME = "blacklist"
        const val LAZY_BLACKLIST_NAME = "lazy-blacklist"
    }

    protected open fun exposeInst() {
        inst = this
    }

    /**
     * The config object
     */
    abstract val config: Config

    /**
     * User cache
     */
    abstract val userCache: UserCache

    /**
     * List manager
     */
    abstract val listMgr: ListManager

    /**
     * The whitelist
     */
    abstract val whitelist: UUIDList

    /**
     * The blacklist
     */
    abstract val blacklist: UUIDList

    /**
     * The whitelist command
     */
    abstract val whitelistCommand: ListCommand

    /**
     * The blacklist command
     */
    abstract val blacklistCommand: ListCommand

    /**
     * If the plugin is enabled
     */
    abstract val enabled: Boolean
}