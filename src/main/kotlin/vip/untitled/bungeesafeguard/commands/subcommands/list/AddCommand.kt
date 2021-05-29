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
import java.util.*

open class AddCommand(
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
     * Add UUID(s) or username(s) to the main list
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     * @param xbox Whether the names in `args` are XBOX tags
     */
    open suspend fun add(sender: CommandSender, args: Array<out String>, xbox: Boolean) {
        var shouldSaveConfig = false
        var shouldSaveCache = false
        for (res in UserUUIDHelper.resolveUUIDs(context, client, args, xbox)) {
            when (val err = res.err) {
                null -> {
                    val nameAndUUID = res.result!!
                    val username = nameAndUUID.name
                    val uuid = nameAndUUID.id
                    if (username != null) {
                        if (userCache.add(uuid, username)) {
                            shouldSaveCache = true
                        }
                    }
                    config.withLock {
                        checkBeforeAdd(sender, res.query, uuid)
                        if (list.addToList(uuid)) {
                            sender.sendMessage(TextComponent("${ChatColor.AQUA}${res.query} ${ChatColor.YELLOW}($uuid) ${ChatColor.AQUA}added to the $listName"))
                            shouldSaveConfig = true
                        } else {
                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${res.query} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is already in the $listName"))
                        }
                    }
                }
                is UserNotFoundException -> sender.sendMessage(TextComponent("${ChatColor.RED}User ${res.query} is not found and therefore cannot be added to the $listName"))
                else -> {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to add ${res.query} to the $listName: $err"))
                    context.logger.warning("Failed to add ${res.query} to the $listName:")
                    err.printStackTrace()
                }
            }
        }
        saveConfigAndCache(sender, shouldSaveConfig, shouldSaveCache)
    }

    /**
     * Add UUID(s) or username(s) to the main list
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    open suspend fun add(sender: CommandSender, args: Array<out String>) = add(sender, args, xbox)

    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return if (args.size > 1) {
            val realArgs = args.copyOfRange(1, args.size)
            Parsed(realArgs, ListAction(isXBOX = xbox, isLazyList = false, isAdd = true))
        } else {
            null
        }
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch { add(sender, realArgs, xbox) }
    }
}