package cyou.untitled.bungeesafeguard.commands.subcommands

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.Config
import cyou.untitled.bungeesafeguard.UserCache

abstract class BSGSubcommand(
    override val context: BungeeSafeguard,
    name: String,
    vararg aliases: String
): Subcommand(context, name, *aliases) {
    protected open val config: Config
        get() = context.config

    protected open val userCache: UserCache
        get() = context.userCache
}