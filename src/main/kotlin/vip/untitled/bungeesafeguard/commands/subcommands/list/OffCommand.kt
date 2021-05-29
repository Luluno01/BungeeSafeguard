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

open class OffCommand(
    context: MetaHolderPlugin,
    name: String,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList,
    vararg aliases: String
) : Base(context, name, client, listMgr, list, false, *aliases) {
    /**
     * Turn off the list
     * @param sender Command sender
     */
    open suspend fun off(sender: CommandSender) {
        config.withLock {
            list.off()
            sender.sendMessage(TextComponent("${ChatColor.GREEN}${listName.capitalize()} ${ChatColor.RED}DISABLED"))
        }
        config.save()
    }

    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return Parsed(emptyArray(), ListAction(isLazyList = false, isAdd = false, isOff = true))
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch { off(sender) }
    }
}