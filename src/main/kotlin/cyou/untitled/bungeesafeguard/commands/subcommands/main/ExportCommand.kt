package cyou.untitled.bungeesafeguard.commands.subcommands.main

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.subcommands.BSGSubcommand
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.storage.Backend
import cyou.untitled.bungeesafeguard.storage.FileManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.io.File
import java.io.IOException

open class ExportCommand(context: BungeeSafeguard) : BSGSubcommand(context, "export", "e") {
    private fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard export <lists.yml>"))
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        if (realArgs.size != 1) return sendUsage(sender)
        val dstConf = realArgs[0]
        GlobalScope.launch(context.dispatcher) {
            doExport(sender, dstConf)
        }
    }

    protected open suspend fun exportList(sender: CommandSender, dst: Backend, src: Backend, path: Array<String>, lazyPath: Array<String>) {
        var ids = 0
        for (id in src.get(path)) {
            if (dst.add(path, id)) ids++
        }
        var names = 0
        for (name in src.get(lazyPath)) {
            if (dst.add(lazyPath, name)) names++
        }
        sender.sendMessage(TextComponent("${ChatColor.AQUA}$ids ${ChatColor.GREEN}UUID(s) and ${ChatColor.AQUA}$names ${ChatColor.GREEN}username(s) exported"))
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    protected open suspend fun doExport(sender: CommandSender, dstConf: String) {
        val src = Backend.getBackend()
        val dstFile = File(context.dataFolder, dstConf)
        val fail = FileManager.withFile(dstFile.path, "BSG-export") {
            return@withFile try {
                if (!it.createNewFile()) {
                    sender.sendMessage(TextComponent("${ChatColor.RED}File \"${dstFile.path}\" already exists, refuse to overwrite"))
                    true
                } else false
            } catch (err: IOException) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Cannot create file \"${dstFile.path}\" for exporting: $err"))
                err.printStackTrace()
                true
            }
        }
        if (fail) return
        withYAMLBackend(sender, context, File(context.dataFolder, dstConf)) { dst ->
            try {
                exportList(
                    sender,
                    dst,
                    src,
                    BungeeSafeguard.WHITELIST,
                    BungeeSafeguard.LAZY_WHITELIST
                )
                exportList(
                    sender,
                    dst,
                    src,
                    BungeeSafeguard.BLACKLIST,
                    BungeeSafeguard.LAZY_BLACKLIST
                )
            } catch (err: Throwable) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to export: $err"))
                err.printStackTrace()
            }
        }
    }
}