package cyou.untitled.bungeesafeguard.commands.subcommands.main

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.Config
import cyou.untitled.bungeesafeguard.commands.subcommands.BSGSubcommand
import cyou.untitled.bungeesafeguard.helpers.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.io.File
import java.io.IOException

open class LoadCommand(context: BungeeSafeguard) : BSGSubcommand(context, "load", "use") {
    open fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard load/use <config-file-name> ${ChatColor.YELLOW}(must be yml file, the extension \".yml\" can be omitted)"))
    }

    override fun execute(sender: CommandSender, realArgs: Array<out String>) {
        GlobalScope.launch(context.dispatcher) {
            if (realArgs.isEmpty()) {
                sendUsage(sender)
                return@launch
            }
            var name = realArgs[0]

            /* Start safety check */
            if (Regex("""(^|[/\\])\.\.[/\\]""").find(name) != null) {
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}Parent folder \"..\" is not allowed in the config path"))
                return@launch
            }
            /* End safety check */

            if (!name.endsWith(".yml")) name += ".yml"
            val configInUseFile = File(context.dataFolder, Config.CONFIG_IN_USE)
            try {
                configInUseFile.writeText(name)
            } catch (err: IOException) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to update file \"${Config.CONFIG_IN_USE}\", aborting"))
                return@launch
            }
            try {
                config.load(sender, name)
            } catch (err: Throwable) {
                sender.sendMessage(TextComponent("${ChatColor.RED}Failed to load config file \"$name\": $err"))
                return@launch
            }
        }
    }
}