package cyou.untitled.bungeesafeguard.commands

import cyou.untitled.bungeesafeguard.BungeeSafeguardImpl
import cyou.untitled.bungeesafeguard.commands.subcommands.SubcommandRegistry
import cyou.untitled.bungeesafeguard.commands.subcommands.list.omitEmpty
import cyou.untitled.bungeesafeguard.commands.subcommands.main.*
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command

@Suppress("MemberVisibilityCanBePrivate")
open class BungeeSafeguard(val context: BungeeSafeguardImpl): Command("bungeesafeguard", "bungeesafeguard.main", "bsg") {
    companion object {
        open class Usage: SubcommandRegistry.Companion.UsageSender {
            override fun sendUsage(sender: CommandSender) {
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
                sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard load/use <config-file-name> ${ChatColor.YELLOW}(must be a yml file, the extension \".yml\" can be omitted)"))
                sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard reload"))
                sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard status"))
                sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard dump"))
                sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard import <old-config-to-import-from.yml> ${ChatColor.YELLOW}(must be a yml file)"))
                sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard merge <old-config-to-merge.yml> ${ChatColor.YELLOW}(must be a yml file)"))
                sender.sendMessage(TextComponent("${ChatColor.AQUA}  /bungeesafeguard export <lists.yml>"))
            }
        }
    }
    protected val usage = Usage()
    protected val cmdReg = SubcommandRegistry(context, usage)

    init {
        cmdReg.registerSubcommand(
            LoadCommand(context),
            ReloadCommand(context),
            StatusCommand(context),
            DumpCommand(context),
            ImportCommand(context),
            MergeCommand(context),
            ExportCommand(context)
        )
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        val fixedArgs = args.omitEmpty()
        cmdReg.getSubcommand(sender, fixedArgs)?.execute(sender, args.sliceArray(IntRange(1, fixedArgs.size - 1)))
    }
}