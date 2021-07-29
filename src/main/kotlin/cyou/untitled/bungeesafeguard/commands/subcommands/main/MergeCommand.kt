package cyou.untitled.bungeesafeguard.commands.subcommands.main

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.subcommands.BSGSubcommand
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.storage.Backend
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.io.File
import java.util.*

open class MergeCommand(context: BungeeSafeguard) : BSGSubcommand(context, "merge", "m") {
    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard merge <old-config-to-merge.yml> ${ChatColor.YELLOW}(must be a yml file)"))
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        if (realArgs.size != 1) return sendUsage(sender)
        val oldConf = realArgs[0]
        GlobalScope.launch(context.dispatcher) {
            doMerge(sender, oldConf)
        }
    }

    protected open suspend fun mergeList(sender: CommandSender, dst: Backend, src: Backend, name: String, lazyName: String, path: Array<String>, lazyPath: Array<String>) {
        var ids = 0
        for (rawId in src.get(path)) {
            val id = try { UUID.fromString(rawId); rawId } catch (err: IllegalArgumentException) {
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}Cannot merge non-UUID record \"$rawId\" in the $name, skipping"))
                continue
            }
            if (dst.add(path, id)) ids++
        }
        var names = 0
        for (username in src.get(lazyPath)) {
            if (username.isEmpty()) {
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}Cannot merge empty username in the $name, skipping"))
                continue
            }
            if (dst.add(lazyPath, username)) names++
        }
        sender.sendMessage(TextComponent("${ChatColor.AQUA}$ids ${ChatColor.GREEN}UUID(s) merged with current $name"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}$names ${ChatColor.GREEN}username(s) merged with current $lazyName"))
    }

    protected open suspend fun doMerge(sender: CommandSender, oldConf: String) {
        val dst = Backend.getBackend()
        withYAMLBackend(sender, context, File(context.dataFolder, oldConf)) { src ->
            try {
                mergeList(
                    sender,
                    dst,
                    src,
                    BungeeSafeguard.WHITELIST_NAME,
                    BungeeSafeguard.LAZY_WHITELIST_NAME,
                    BungeeSafeguard.WHITELIST,
                    BungeeSafeguard.LAZY_WHITELIST
                )
                mergeList(
                    sender,
                    dst,
                    src,
                    BungeeSafeguard.BLACKLIST_NAME,
                    BungeeSafeguard.LAZY_BLACKLIST_NAME,
                    BungeeSafeguard.BLACKLIST,
                    BungeeSafeguard.LAZY_BLACKLIST
                )
            } catch (err: Throwable) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to merge: $err"))
                err.printStackTrace()
            }
        }
    }
}