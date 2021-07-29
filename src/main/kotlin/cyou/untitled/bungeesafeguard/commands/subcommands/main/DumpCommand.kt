package cyou.untitled.bungeesafeguard.commands.subcommands.main

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.subcommands.BSGSubcommand
import cyou.untitled.bungeesafeguard.helpers.ListDumper
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.storage.Backend
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.util.*

open class DumpCommand(context: BungeeSafeguard) : BSGSubcommand(context, "dump") {
    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch(context.dispatcher) {
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Using config file ${ChatColor.AQUA}${config.configInUse}"))
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Using backend ${ChatColor.AQUA}${Backend.getBackend()}"))
            for (list in context.listMgr.lists) {
                ListDumper.printListStatus(
                    sender,
                    list.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    list.enabled
                )
                ListDumper.printListsContent(sender, list.path, list.lazyPath, userCache)
            }
        }
    }
}