package cyou.untitled.bungeesafeguard.commands.subcommands.main

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.subcommands.BSGSubcommand
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent

open class ReloadCommand(context: BungeeSafeguard) : BSGSubcommand(context, "reload") {
    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch(context.dispatcher) {
            coroutineScope {
                launch {
                    try {
                        config.reload(sender)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}BungeeSafeguard reloaded"))
                    } catch (err: Throwable) {
                        sender.sendMessage(TextComponent("${ChatColor.RED}Failed to reload: $err"))
                    }
                }
                launch {
                    context.userCache.reload()
                }
            }
        }
    }
}