package cyou.untitled.bungeesafeguard.commands.subcommands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin

abstract class Subcommand(
    open val context: Plugin,
    open val name: String,
    open vararg val aliases: String
) {
    /**
     * Execute the subcommand
     * @param sender The sender of this command
     * @param realArgs Real args for this command
     */
    abstract fun execute(sender: CommandSender, realArgs: Array<out String>)
}