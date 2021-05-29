package vip.untitled.bungeesafeguard.commands.subcommands

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeesafeguard.Config
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import vip.untitled.bungeesafeguard.UserCache
import java.io.IOException

abstract class Subcommand(
    val context: MetaHolderPlugin,
    val name: String,
    vararg val aliases: String
) {
    protected open val config: Config
        get() = context.config!!

    protected open val userCache: UserCache
        get() = context.userCache!!

    protected open suspend fun saveConfigAndCache(sender: CommandSender, shouldSaveConfig: Boolean, shouldSaveCache: Boolean) {
        val configSaveJob = GlobalScope.launch {
            try {
                if (shouldSaveConfig) {
                    config.save()  // With `withLock` inside
                }
            } catch (err: IOException) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Cannot save config file: $err"))
                context.logger.warning("Cannot save config file:")
                err.printStackTrace()
            }
        }
        val cacheSaveJob = GlobalScope.launch {
            try {
                if (shouldSaveCache) {
                    userCache.withLock {
                        userCache.save()
                    }
                }
            } catch (err: IOException) {}
        }
        joinAll(configSaveJob, cacheSaveJob)
    }

    /**
     * Execute the subcommand
     * @param sender The sender of this command
     * @param realArgs Real args for this command
     */
    abstract fun execute(sender: CommandSender, realArgs: Array<out String>)
}