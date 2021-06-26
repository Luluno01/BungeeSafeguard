package cyou.untitled.bungeesafeguard.commands.subcommands.list

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.ListCommand
import cyou.untitled.bungeesafeguard.commands.subcommands.Subcommand
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import cyou.untitled.bungeesafeguard.list.joinLazyListName
import cyou.untitled.bungeesafeguard.list.joinListName
import io.ktor.client.*
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.util.*

abstract class Base(
    context: BungeeSafeguard,
    name: ListCommand.Companion.SubcommandName,
    protected val client: HttpClient,
    protected val listMgr: ListManager,
    protected val list: UUIDList,
    val confirmable: Boolean = true
) : Subcommand(context, name.cmdName, *name.aliases) {
    /**
     * Name of this list
     */
    protected open val listName: String
        get() = list.name

    /**
     * Name of the lazy list
     */
    protected open val lazyName: String
        get() = list.lazyName

    protected open suspend fun checkBeforeAdd(sender: CommandSender, query: String, uuid: UUID) {
        val higher = listMgr.inListsWithHigherPriority(uuid, list)
        if (higher.isNotEmpty()) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}$query ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is already in ${higher.joinListName()}, whose priority is higher than $listName"))
        }
        val lower = listMgr.inListsWithLowerPriority(uuid, list)
        if (lower.isNotEmpty()) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}$query ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is already in ${lower.joinListName()}, whose priority is lower than $listName"))
        }
    }

    protected open suspend fun checkBeforeLazyAdd(sender: CommandSender, username: String) {
        val higher = listMgr.inLazyListsWithHigherPriority(username, list)
        if (higher.isNotEmpty()) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}$username ${ChatColor.YELLOW}is already in ${higher.joinLazyListName()}, whose priority is higher than $listName"))
        }
        val lower = listMgr.inLazyListsWithLowerPriority(username, list)
        if (lower.isNotEmpty()) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}$username ${ChatColor.YELLOW}is already in ${lower.joinLazyListName()}, whose priority is lower than $listName"))
        }
    }

    /**
     * Parse the raw argument whose format will be currently hardcoded
     * @param sender The command sender
     * @param args Raw arguments
     * @return A `Parsed` object, which contains real args and a list action,
     * if the arguments are valid; `null` if the arguments are invalid,
     * in which case the usage will be sent back to the command sender by the caller,
     * i.e., the parent command
     */
    abstract fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed?
}
