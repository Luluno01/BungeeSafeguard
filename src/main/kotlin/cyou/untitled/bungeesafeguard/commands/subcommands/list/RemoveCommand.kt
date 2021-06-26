package cyou.untitled.bungeesafeguard.commands.subcommands.list

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.ListCommand
import cyou.untitled.bungeesafeguard.helpers.UserNotFoundException
import cyou.untitled.bungeesafeguard.helpers.UserUUIDHelper
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import io.ktor.client.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent

open class RemoveCommand(
    context: BungeeSafeguard,
    name: ListCommand.Companion.SubcommandName,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList,
    /**
     * Whether a this add command is for XBOX
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected val xbox: Boolean
) : Base(context, name, client, listMgr, list, true) {
    /**
     * Remove UUID(s) or username(s) from the list
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     * @param xbox Whether the names in `args` are XBOX tags
     */
    open suspend fun remove(sender: CommandSender, args: Array<out String>, xbox: Boolean) {
        UserUUIDHelper.resolveUUIDs(context, client, args, xbox) {
            when (val err = it.err) {
                null -> {
                    val nameAndUUID = it.result!!
                    val username = nameAndUUID.name
                    val uuid = nameAndUUID.id
                    if (list.remove(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${it.query} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}removed from the $listName"))
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${it.query} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is not in the $listName"))
                    }
                    if (username != null && !listMgr.inAnyList(uuid)) {
                        userCache.removeAndSave(uuid)
                    }
                }
                is UserNotFoundException -> sender.sendMessage(TextComponent("${ChatColor.RED}User ${it.query} is not found and therefore cannot be removed from the $listName"))
                else -> {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to remove ${it.query} from the $listName: $err"))
                    context.logger.warning("Failed to remove ${it.query} from the $listName:")
                    err.printStackTrace()
                }
            }
        }
    }

    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return if (args.size > 1) {
            val realArgs = args.copyOfRange(1, args.size)
            Parsed(realArgs, ListAction(isXBOX = xbox, isLazyList = false, isAdd = false))
        } else {
            null
        }
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch(context.dispatcher) { remove(sender, realArgs, xbox) }
    }
}