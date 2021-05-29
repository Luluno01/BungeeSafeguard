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
import vip.untitled.bungeesafeguard.helpers.TypedJSON
import java.io.File
import java.io.IOException

open class ImportCommand(
    context: MetaHolderPlugin,
    name: String,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList,
    vararg aliases: String
) : AddCommand(context, name, client, listMgr, list, false, *aliases) {
    companion object {
        suspend fun loadUUIDsInJSONArray(sender: CommandSender, file: File): Array<String>? {
            val json = try {
                TypedJSON.fromFile(file)
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
        GlobalScope.launch { import(sender, realArgs[0]) }
    }
}