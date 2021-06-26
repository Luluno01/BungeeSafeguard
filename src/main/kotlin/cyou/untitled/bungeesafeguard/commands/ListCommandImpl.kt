package cyou.untitled.bungeesafeguard.commands

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.list.ListManager
import io.ktor.client.*
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import cyou.untitled.bungeesafeguard.Config
import cyou.untitled.bungeesafeguard.commands.ListCommand.Companion.SubcommandName.*
import cyou.untitled.bungeesafeguard.commands.subcommands.list.*
import cyou.untitled.bungeesafeguard.list.UUIDList
import java.io.File

open class ListCommandImpl(
    context: BungeeSafeguard,
    client: HttpClient,
    listMgr: ListManager,
    list: UUIDList,
    name: String, permission: String, vararg aliases: String):
    ListCommand(context, client, listMgr, list, name, permission, *aliases) {
    @Suppress("MemberVisibilityCanBePrivate")
    protected val subcommands: MutableList<Base> = mutableListOf(
        ImportCommand(context, IMPORT, client, listMgr, list),
        AddCommand(context, ADD, client, listMgr, list, false),
        AddCommand(context, X_ADD, client, listMgr, list, true),
        LazyAddCommand(context, LAZY_ADD, client, listMgr, list),
        RemoveCommand(context, REMOVE, client, listMgr, list, false),
        RemoveCommand(context, X_REMOVE, client, listMgr, list, true),
        LazyRemoveCommand(context, LAZY_REMOVE, client, listMgr, list),
        OnCommand(context, ON, client, listMgr, list),
        OffCommand(context, OFF, client, listMgr, list),
        DumpCommand(context, LIST, client, listMgr, list)
    )
    @Suppress("MemberVisibilityCanBePrivate")
    protected val subcommandMap = mutableMapOf<String, Base>()
    init {
        for (subcommand in subcommands) {
            assert(!subcommandMap.containsKey(subcommand.name)) { "Conflicting subcommand name ${subcommand.name}" }
            subcommandMap[subcommand.name] = subcommand
            for (alias in subcommand.aliases) {
                assert(!subcommandMap.containsKey(alias)) { "Conflicting subcommand alias/name $alias" }
                subcommandMap[alias] = subcommand
            }
        }
    }

    protected open val config: Config
        get() = context.config

    /**
     * Name of this list
     */
    protected open val listName: String
        get() = list.name

    protected open val lazyName: String
        get() = list.lazyName

    /**
     * Send usage to a command sender
     * @param sender Command sender
     */
    open fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /$name import <path to whitelist.json or banned-players.json>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For normal Mojang players: /$name <add/remove/rm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For XBOX Live players: /$name <x-add/xadd/x-remove/x-remove/x-rm/xrm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For both Mojang and XBOX players: /$name <lazy-add/lazy-remove/lazyadd/ladd/lazyremove/lremove/lrm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /$name <on/off>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /$name <list/ls/show/dump>"))
    }

    /**
     * Send confirm message to the command sender
     */
    open fun sendConfirmMessage(sender: CommandSender, subcommand: Base, parsed: Parsed) {
        val realArgs = parsed.realArgs
        val action = parsed.action
        if (action.isImport) {
            sender.sendMessage(
                TextComponent("${ChatColor.YELLOW}Are you sure you want to ${ChatColor.AQUA}${ChatColor.BOLD}import UUIDs " +
                        "${ChatColor.RESET}${ChatColor.YELLOW}from the following ${ChatColor.AQUA}${ChatColor.BOLD}external JSON file " +
                        "${ChatColor.RESET}${ChatColor.YELLOW}to the ${ChatColor.AQUA}${ChatColor.BOLD}$listName " +
                        "${ChatColor.RESET}${ChatColor.YELLOW}in the config file \"${ChatColor.AQUA}${ChatColor.BOLD}${config.configInUse}${ChatColor.RESET}${ChatColor.YELLOW}\"?\n" +
                        "${ChatColor.AQUA}${ChatColor.BOLD}  ${File(realArgs[0]).absolutePath}")
            )
        } else {
            sender.sendMessage(
                TextComponent("${ChatColor.YELLOW}Are you sure you want to ${ChatColor.AQUA}${ChatColor.BOLD}${if (action.isAdd) "add" else "remove"} " +
                        "${ChatColor.RESET}${ChatColor.YELLOW}the following ${ChatColor.AQUA}${ChatColor.BOLD}${if (action.isXBOX) "XBOX Live" else "Minecraft"} player(s) " +
                        "${ChatColor.RESET}${ChatColor.YELLOW}${if (action.isAdd) "to" else "from"} the ${ChatColor.AQUA}${ChatColor.BOLD}${if (action.isLazyList) lazyName else listName} " +
                        "${ChatColor.RESET}${ChatColor.YELLOW}in the config file \"${ChatColor.AQUA}${ChatColor.BOLD}${config.configInUse}${ChatColor.RESET}${ChatColor.YELLOW}\"?\n" +
                        "${ChatColor.AQUA}${ChatColor.BOLD}" + realArgs.joinToString("\n") { "  $it" })
            )
        }
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Please use ${ChatColor.AQUA}/$name confirm ${ChatColor.YELLOW}in 10s to confirm"))
    }

    protected open fun possiblyDoAfterConfirmation(sender: CommandSender, subcommand: Base, parsed: Parsed) {
        if (config.confirm && subcommand.confirmable) {
            sendConfirmMessage(sender, subcommand, parsed)
            confirm(sender, { subcommand.execute(sender, parsed.realArgs) })
        } else {
            subcommand.execute(sender, parsed.realArgs)  // Do it now
        }
    }

    protected open fun onConfirm(sender: CommandSender) {
        if (!confirmed(sender)) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}Nothing to confirm, it might have expired"))
        }
    }

    override fun getSubcommand(name: Companion.SubcommandName): Base? = subcommandMap[name.cmdName]
    override fun getSubcommand(name: String): Base? = subcommandMap[name]

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
        } else {
            val subcommandString = args[0]
            if (subcommandString == "confirm") {
                onConfirm(sender)
                return
            }
            val subcommand = subcommandMap[subcommandString]
            if (subcommand == null) {
                sendUsage(sender)
                return
            }
            when (val parsed = subcommand.parseArgs(sender, args)) {
                null -> sendUsage(sender)
                else -> possiblyDoAfterConfirmation(sender, subcommand, parsed)
            }
        }
    }
}