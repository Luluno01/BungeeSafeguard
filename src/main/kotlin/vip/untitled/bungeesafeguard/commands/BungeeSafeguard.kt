package vip.untitled.bungeesafeguard.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import vip.untitled.bungeesafeguard.Config
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import vip.untitled.bungeesafeguard.helpers.ListDumper
import java.io.File
import java.io.IOException

open class BungeeSafeguard(val context: MetaHolderPlugin): Command("bungeesafeguard", "bungeesafeguard.main", "bsg") {
    open fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard load/use <config-file-name> ${ChatColor.YELLOW}(must be yml file, the extension \".yml\" can be omitted)"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard reload"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard status"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard dump"))
    }

    /**
     * Handle subcommand load/use
     */
    protected open suspend fun handleLoad(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sendUsage(sender)
            return
        }
        var name = args[1]

        /* Start safety check */
        if (Regex("""(^|[/\\])\.\.[/\\]""").find(name) != null) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}Parent folder \"..\" is not allowed in the config path"))
            return
        }
        /* End safety check */

        if (!name.endsWith(".yml")) name += ".yml"
        val config = context.config!!
        val configInUseFile = File(context.dataFolder, Config.CONFIG_IN_USE)
        config.withLock {
            try {
                withContext(Dispatchers.IO) { configInUseFile.writeText(name) }
            } catch (err: IOException) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to update file \"${Config.CONFIG_IN_USE}\", aborting"))
                return@withLock
            }
            try {
                config.reload(sender, name)
            } catch (err: Throwable) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to load config file \"$name\""))
            }
        }
    }

    /**
     * Handle subcommand reload
     */
    protected open suspend fun handleReload(sender: CommandSender, args: Array<out String>) {
        val config = context.config!!
        config.withLock {
            try {
                config.reload(sender)
                sender.sendMessage(TextComponent("${ChatColor.GREEN}BungeeSafeguard reloaded"))
            } catch (err: Throwable) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to reload: $err"))
            }
        }
        val cache = context.userCache!!
        cache.withLock {
            cache.reload()
        }
    }

    /**
     * Handle subcommand status
     */
    protected open suspend fun handleStatus(sender: CommandSender, args: Array<out String>) {
        val config = context.config!!
        config.withLock {
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Using config file ${ChatColor.AQUA}${config.configInUse}"))
            for (list in config.listMgr.lists) {
                sender.sendMessage(TextComponent("${ChatColor.GREEN}${list.name.capitalize()} ${if (list.enabled) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
            }
        }
    }

    /**
     * Handle subcommand dump
     */
    protected open suspend fun handleDump(sender: CommandSender, args: Array<out String>) {
        val config = context.config!!
        config.withLock {
            val cache = context.userCache!!
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Using config file ${ChatColor.AQUA}${config.configInUse}"))
            cache.withLock {
                for (list in config.listMgr.lists) {
                    ListDumper.printListStatus(sender, list.name.capitalize(), list.enabled)
                    ListDumper.printListsContent(sender, list.lazyList, list.list, cache)
                }
            }
        }
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
            return
        }
        // This class is an exception that can access `config.*list` directly
        when (args[0]) {
            "load", "use" -> runBlocking { handleLoad(sender, args) }
            "reload" -> runBlocking { handleReload(sender, args) }
            "status" -> runBlocking { handleStatus(sender, args) }
            "dump" -> runBlocking { handleDump(sender, args) }
            else -> sendUsage(sender)
        }
    }
}