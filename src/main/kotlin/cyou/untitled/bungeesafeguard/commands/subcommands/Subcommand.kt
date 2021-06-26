package cyou.untitled.bungeesafeguard.commands.subcommands

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.Config
import cyou.untitled.bungeesafeguard.UserCache
import net.md_5.bungee.api.CommandSender

abstract class Subcommand(
    val context: BungeeSafeguard,
    val name: String,
    vararg val aliases: String
) {
    protected open val config: Config
        get() = context.config

    protected open val userCache: UserCache
        get() = context.userCache

    /**
     * Execute the subcommand
     * @param sender The sender of this command
     * @param realArgs Real args for this command
     */
    abstract fun execute(sender: CommandSender, realArgs: Array<out String>)
}