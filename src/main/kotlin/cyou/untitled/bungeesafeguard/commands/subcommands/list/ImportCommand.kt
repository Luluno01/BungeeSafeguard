package cyou.untitled.bungeesafeguard.commands.subcommands.list

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.ListCommand
import cyou.untitled.bungeesafeguard.helpers.TypedJSON
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import io.ktor.client.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.io.File
import java.io.IOException

open class ImportCommand(
    context: BungeeSafeguard,
    name: ListCommand.Companion.SubcommandName,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList
) : AddCommand(context, name, client, listMgr, list, false) {
    companion object {
        fun loadUUIDsInJSONArray(sender: CommandSender, file: File): Array<String>? {
            val json = try {
                TypedJSON.fromFileSync(file)
            } catch (err: IOException) {
                sender.sendMessage(TextComponent("${ChatColor.RED}\"${file.absolutePath}\" does not exists, or is unreadable"))
                return null
            }
            if (!json.json.isJsonArray) {
                sender.sendMessage(TextComponent("${ChatColor.RED}The content of \"${file.absolutePath}\" must be a JSON array"))
                return null
            }
            val uuids = arrayListOf<String>()
            for ((i, elem) in json.json.asJsonArray.withIndex()) {
                val uuid = try {
                    val obj = elem.asJsonObject
                    obj.get("uuid").asString
                } catch (err: Throwable) {
                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}Element with index \"$i\" is invalid, ignored"))
                    continue
                }
                uuids.add(uuid)
            }
            return uuids.toTypedArray()
        }
    }

    /**
     * Import UUIDs from external list and merge with current list
     */
    open suspend fun import(sender: CommandSender, path: String) {
        val ids = loadUUIDsInJSONArray(sender, File(path)) ?: return
        add(sender, ids)
    }

    override fun parseArgs(sender: CommandSender, args: Array<out String>): Parsed? {
        return if (args.size > 1) {
            val realArgs = args.copyOfRange(1, 2)
            Parsed(realArgs, ListAction(isLazyList = false, isAdd = false, isImport = true))
        } else {
            null
        }
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch(context.dispatcher) { import(sender, realArgs[0]) }
    }
}