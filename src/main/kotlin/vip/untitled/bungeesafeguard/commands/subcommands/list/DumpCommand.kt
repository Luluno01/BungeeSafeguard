package vip.untitled.bungeesafeguard.commands.subcommands.list

import io.ktor.client.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.CommandSender
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import vip.untitled.bungeesafeguard.config.ListManager
import vip.untitled.bungeesafeguard.config.UUIDList
import vip.untitled.bungeesafeguard.helpers.ListDumper

open class DumpCommand(
    context: MetaHolderPlugin,
    name: String,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList,
    vararg aliases: String
) : Base(context, name, client, listMgr, list, false, *aliases) {
    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return Parsed(emptyArray(), ListAction(isLazyList = false, isAdd = false, isDump = true))
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch {
            config.withLock {
                ListDumper.printListStatus(sender, listName.capitalize(), list.enabled)
                ListDumper.printListsContent(sender, list.lazyList, list.list, userCache)
            }
        }
    }
}