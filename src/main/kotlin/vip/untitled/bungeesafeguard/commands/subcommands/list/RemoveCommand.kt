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
import vip.untitled.bungeesafeguard.helpers.UserNotFoundException
import vip.untitled.bungeesafeguard.helpers.UserUUIDHelper

open class RemoveCommand(
    context: MetaHolderPlugin,
    name: String,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList,
    /**
     * Whether a this add command is for XBOX
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected val xbox: Boolean,
    vararg aliases: String
) : Base(context, name, client, listMgr, list, true, *aliases) {
    /**
     * Remove UUID(s) or username(s) from the list
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     * @param xbox Whether the names in `args` are XBOX tags
     */
    open suspend fun remove(sender: CommandSender, args: Array<out String>, xbox: Boolean) {
        var shouldSaveConfig = false
        var shouldSaveCache = false
        for (res in UserUUIDHelper.resolveUUIDs(context, client, args, xbox)) {
            when (val err = res.err) {
                null -> {
                    val nameAndUUID = res.result!!
                    val username = nameAndUUID.name
                    val uuid = nameAndUUID.id
                    if (username != null) {
                        if (userCache.remove(uuid) != null) {
                            shouldSaveCache = true
                        }
                    }
                    config.withLock {
                        if (list.removeFromList(uuid)) {
                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${res.query} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}removed from the $listName"))
                            shouldSaveConfig = true
                        } else {
                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${res.query} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is not in the $listName"))
                        }
                    }
                }
                is UserNotFoundException -> sender.sendMessage(TextComponent("${ChatColor.RED}User ${res.query} is not found and therefore cannot be removed from the $listName"))
                else -> {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to remove ${res.query} from the $listName: $err"))
                    context.logger.warning("Failed to remove ${res.query} from the $listName:")
                    err.printStackTrace()
                }
            }
        }
        saveConfigAndCache(sender, shouldSaveConfig, shouldSaveCache)
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
        GlobalScope.launch { remove(sender, realArgs, xbox) }
    }
}