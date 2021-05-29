package vip.untitled.bungeesafeguard.commands.subcommands.list

import io.ktor.client.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import vip.untitled.bungeesafeguard.config.ListManager
import vip.untitled.bungeesafeguard.config.UUIDList
import java.io.IOException
import java.util.*

open class LazyRemoveCommand(
    context: MetaHolderPlugin,
    name: String,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList,
    vararg aliases: String
) : Base(context, name, client, listMgr, list, true, *aliases) {
    /**
     * Lazy-remove UUID(s) or username(s) from the list
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    open suspend fun lazyRemove(sender: CommandSender, args: Array<out String>) {
        var shouldSaveConfig = false
        config.withLock {
            for (usernameOrUUID in args) {
                try {
                    val uuid = UUID.fromString(usernameOrUUID)
                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}$usernameOrUUID is a UUID and will be added to the $listName"))
                    checkBeforeAdd(sender, usernameOrUUID, uuid)
                    if (list.removeFromList(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.AQUA}$usernameOrUUID ${ChatColor.YELLOW}removed from the $listName"))
                        shouldSaveConfig = true
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.AQUA}$usernameOrUUID ${ChatColor.YELLOW}is not in the $listName"))
                    }
                } catch (e: IllegalArgumentException) {
                    if (list.removeFromLazyList(usernameOrUUID)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}$usernameOrUUID removed from the $lazyName"))
                        shouldSaveConfig = true
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}$usernameOrUUID is not in the $lazyName"))
                    }
                }
            }
        }
        if (shouldSaveConfig) {
            try {
                config.save()
            } catch (err: IOException) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Cannot save config file: $err"))
                context.logger.warning("Cannot save config file:")
                err.printStackTrace()
            }
        }
    }

    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return if (args.size > 1) {
            val realArgs = args.copyOfRange(1, args.size)
            Parsed(realArgs, ListAction(isLazyList = true, isAdd = false))
        } else {
            null
        }
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch { lazyRemove(sender, realArgs) }
    }
}