package cyou.untitled.bungeesafeguard.commands

import cyou.untitled.bungeesafeguard.BungeeSafeguardImpl
import cyou.untitled.bungeesafeguard.Config
import cyou.untitled.bungeesafeguard.helpers.ListDumper
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import cyou.untitled.bungeesafeguard.storage.Backend
import kotlinx.coroutines.*
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import java.io.File
import java.io.IOException
import java.util.*

open class BungeeSafeguard(val context: BungeeSafeguardImpl): Command("bungeesafeguard", "bungeesafeguard.main", "bsg") {
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
        val config = context.config
        val configInUseFile = File(context.dataFolder, Config.CONFIG_IN_USE)
        try {
            withContext(context.dispatcher) { configInUseFile.writeText(name) }
        } catch (err: IOException) {
            sender.sendMessage(TextComponent("${ChatColor.RED}Failed to update file \"${Config.CONFIG_IN_USE}\", aborting"))
            return
        }
        try {
            config.load(sender, name)
        } catch (err: Throwable) {
            sender.sendMessage(TextComponent("${ChatColor.RED}Failed to load config file \"$name\": $err"))
            return
        }
    }

    /**
     * Handle subcommand reload
     */
    protected open suspend fun handleReload(sender: CommandSender, args: Array<out String>) {
        val config = context.config
        coroutineScope {
            launch {
                try {
                    config.reload(sender)
                    // Update lists
                    context.whitelist.message
                    sender.sendMessage(TextComponent("${ChatColor.GREEN}BungeeSafeguard reloaded"))
                } catch (err: Throwable) {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to reload: $err"))
                }
            }
            launch {
                context.userCache.reload()
            }
        }
    }

    /**
     * Handle subcommand status
     */
    protected open suspend fun handleStatus(sender: CommandSender, args: Array<out String>) {
        val config = context.config
        sender.sendMessage(TextComponent("${ChatColor.GREEN}Using config file ${ChatColor.AQUA}${config.configInUse}"))
        sender.sendMessage(TextComponent("${ChatColor.GREEN}Using backend ${ChatColor.AQUA}${Backend.getBackend()}"))
        for (list in context.listMgr.lists) {
            sender.sendMessage(TextComponent("${ChatColor.GREEN}${list.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }} ${if (list.enabled) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
        }
    }

    /**
     * Handle subcommand dump
     */
    protected open suspend fun handleDump(sender: CommandSender, args: Array<out String>) {
        val config = context.config
        val cache = context.userCache
        sender.sendMessage(TextComponent("${ChatColor.GREEN}Using config file ${ChatColor.AQUA}${config.configInUse}"))
        sender.sendMessage(TextComponent("${ChatColor.GREEN}Using backend ${ChatColor.AQUA}${Backend.getBackend()}"))
        for (list in context.listMgr.lists) {
            ListDumper.printListStatus(
                sender,
                list.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                list.enabled
            )
            ListDumper.printListsContent(sender, list.path, list.lazyPath, cache)
        }
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
            return
        }
        GlobalScope.launch(context.dispatcher) {
            when (args[0]) {
                "load", "use" -> handleLoad(sender, args)
                "reload" -> handleReload(sender, args)
                "status" -> handleStatus(sender, args)
                "dump" -> handleDump(sender, args)
                else -> sendUsage(sender)
            }
        }
    }
}