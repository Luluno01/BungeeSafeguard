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

open class ImportCommand(context: BungeeSafeguard) : BSGSubcommand(context, "import", "i") {
    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard import <old-config-to-import-from.yml> ${ChatColor.YELLOW}(must be a yml file)"))
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        if (realArgs.size != 1) return sendUsage(sender)
        val oldConf = realArgs[0]
        GlobalScope.launch(context.dispatcher) {
            doImport(sender, oldConf)
        }
    }

    protected open suspend fun importList(sender: CommandSender, dst: Backend, src: Backend, name: String, lazyName: String, path: Array<String>, lazyPath: Array<String>) {
        var ids = 0
        for (rawId in src.get(path)) {
            val id = try { UUID.fromString(rawId); rawId } catch (err: IllegalArgumentException) {
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}Cannot import non-UUID record \"$rawId\" in the $name, skipping"))
                continue
            }
            if (dst.add(path, id)) ids++
        }
        var names = 0
        for (username in src.get(lazyPath)) {
            if (username.isEmpty()) {
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}Cannot import empty username in the $name, skipping"))
                continue
            }
            if (dst.add(lazyPath, username)) names++
        }
        sender.sendMessage(TextComponent("${ChatColor.AQUA}$ids ${ChatColor.GREEN}UUID(s) imported to the $name"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}$names ${ChatColor.GREEN}username(s) imported to the $lazyName"))
    }

    protected open suspend fun doImport(sender: CommandSender, oldConf: String) {
        val dst = Backend.getBackend()
        if (dst.getSize(BungeeSafeguard.WHITELIST) > 0 || dst.getSize(BungeeSafeguard.LAZY_WHITELIST) > 0 || dst.getSize(BungeeSafeguard.BLACKLIST) > 0 || dst.getSize(BungeeSafeguard.LAZY_BLACKLIST) > 0) {
            sender.sendMessage(TextComponent("${ChatColor.RED}Current backend non-empty, reject importing"))
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}If you want to force import, please use /bungeesafeguard merge"))
            return
        }
        withYAMLBackend(sender, context, File(context.dataFolder, oldConf)) { src ->
            try {
                importList(
                    sender,
                    dst,
                    src,
                    BungeeSafeguard.WHITELIST_NAME,
                    BungeeSafeguard.LAZY_WHITELIST_NAME,
                    BungeeSafeguard.WHITELIST,
                    BungeeSafeguard.LAZY_WHITELIST
                )
                importList(
                    sender,
                    dst,
                    src,
                    BungeeSafeguard.BLACKLIST_NAME,
                    BungeeSafeguard.LAZY_BLACKLIST_NAME,
                    BungeeSafeguard.BLACKLIST,
                    BungeeSafeguard.LAZY_BLACKLIST
                )
            } catch (err: Throwable) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to import: $err"))
                err.printStackTrace()
            }
        }
    }
}