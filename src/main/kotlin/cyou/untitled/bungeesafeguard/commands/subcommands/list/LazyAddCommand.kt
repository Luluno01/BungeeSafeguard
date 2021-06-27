package cyou.untitled.bungeesafeguard.commands.subcommands.list

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.ListCommand
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.util.*

open class LazyAddCommand(
    context: BungeeSafeguard,
    name: ListCommand.Companion.SubcommandName,
    listMgr: ListManager,
    list: UUIDList
) : Base(context, name, listMgr, list, true) {
    /**
     * Lazy-add UUID(s) or username(s) to the list
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    open suspend fun lazyAdd(sender: CommandSender, args: Array<out String>) {
        for (usernameOrUUID in args) {
            try {
                val uuid = UUID.fromString(usernameOrUUID)
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}$usernameOrUUID is a UUID and will be added to the $listName"))
                checkBeforeAdd(sender, usernameOrUUID, uuid)
                if (list.add(uuid)) {
                    sender.sendMessage(TextComponent("${ChatColor.GREEN}$uuid added to the $listName"))
                } else {
                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}$usernameOrUUID is already in the $listName"))
                }
            } catch (e: IllegalArgumentException) {
                checkBeforeLazyAdd(sender, usernameOrUUID)
                if (list.lazyAdd(usernameOrUUID)) {
                    sender.sendMessage(TextComponent("${ChatColor.GREEN}$usernameOrUUID added to the $lazyName"))
                } else {
                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}$usernameOrUUID is already in the $lazyName"))
                }
            }
        }
    }

    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return if (args.size > 1) {
            val realArgs = args.copyOfRange(1, args.size)
            Parsed(realArgs, ListAction(isLazyList = true, isAdd = true))
        } else {
            null
        }
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch(context.dispatcher) { lazyAdd(sender, realArgs) }
    }
}