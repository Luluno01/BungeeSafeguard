package cyou.untitled.bungeesafeguard.commands.subcommands.list

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.ListCommand
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import io.ktor.client.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.util.*

open class OffCommand(
    context: BungeeSafeguard,
    name: ListCommand.Companion.SubcommandName,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList
) : Base(context, name, client, listMgr, list, false) {
    /**
     * Turn off the list
     * @param sender Command sender
     */
    open suspend fun off(sender: CommandSender) {
        list.off(sender)
        sender.sendMessage(TextComponent("${ChatColor.GREEN}${listName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }} ${ChatColor.RED}DISABLED"))
    }

    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return Parsed(emptyArray(), ListAction(isLazyList = false, isAdd = false, isOff = true))
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch { off(sender) }
    }
}