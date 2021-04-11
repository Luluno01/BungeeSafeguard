package vip.untitled.bungeesafeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import vip.untitled.bungeesafeguard.Config
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import vip.untitled.bungeesafeguard.helpers.ListDumper
import java.io.File
import java.io.IOException
import java.util.*

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
    protected open fun handleLoad(sender: CommandSender, args: Array<out String>) {
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
        context.proxy.scheduler.runAsync(context) {
            synchronized (config) {
                val configInUseFile = File(context.dataFolder, Config.CONFIG_IN_USE)
                try {
                    configInUseFile.writeText(name)
                } catch (err: IOException) {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to update file \"${Config.CONFIG_IN_USE}\", aborting"))
                    return@runAsync
                }
                try {
                    config.reload(sender, name)
                } catch (err: Throwable) {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to load config file \"$name\""))
                }
            }
        }
    }

    /**
     * Handle subcommand reload
     */
    protected open fun handleReload(sender: CommandSender, args: Array<out String>) {
        context.proxy.scheduler.runAsync(context) {
            try {
                context.config.reload(sender)
                sender.sendMessage(TextComponent("${ChatColor.GREEN}BungeeSafeguard reloaded"))
            } catch (e: Throwable) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to reload: $e"))
            }
            context.userCache.reload()
        }
    }

    /**
     * Handle subcommand status
     */
    protected open fun handleStatus(sender: CommandSender, args: Array<out String>) {
        val config = context.config
        synchronized (config) {
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Using config file ${ChatColor.AQUA}${config.configInUse}"))
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Whitelist ${if (config.enableWhitelist) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Blacklist ${if (config.enableBlacklist) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
        }
    }

    /**
     * Handle subcommand dump
     */
    protected open fun handleDump(sender: CommandSender, args: Array<out String>) {
        val config = context.config
        synchronized (config) {
            val cache = context.userCache
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Using config file ${ChatColor.AQUA}${config.configInUse}"))
            ListDumper.printListStatus(sender, "Whitelist", config.enableWhitelist)
            ListDumper.printListsContent(sender, config.lazyWhitelist, config.whitelist, cache)
            ListDumper.printListStatus(sender, "Blacklist", config.enableBlacklist)
            ListDumper.printListsContent(sender, config.lazyBlacklist, config.blacklist, cache)
        }
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
            return
        }
        // This class is an exception that can access `config.*list` directly
        when (args[0]) {
            "load", "use" -> handleLoad(sender, args)
            "reload" -> handleReload(sender, args)
            "status" -> handleStatus(sender, args)
            "dump" -> handleDump(sender, args)
            else -> sendUsage(sender)
        }
    }
}