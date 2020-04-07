package vip.untitled.bungeeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import vip.untitled.bungeeguard.ConfigHolderPlugin

open class BungeeGuard(val context: ConfigHolderPlugin): Command("bungeeguard", "bungeeguard.main", "bg") {
    open fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /bungeeguard reload"))
    }
    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
            return
        }
        // This class is an exception that can access `config.*list` directly
        when (args[0]) {
            "reload" -> context.proxy.scheduler.runAsync(context) {
                try {
                    context.config.reload()
                    sender.sendMessage(TextComponent("${ChatColor.GREEN}BungeeGuard reloaded"))
                } catch (e: Throwable) {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to reload: $e"))
                }
            }
            "status" -> {
                val config = context.config
                sender.sendMessage(TextComponent("${ChatColor.GREEN}Whitelist ${if (config.enableWhitelist) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
                sender.sendMessage(TextComponent("${ChatColor.GREEN}Blacklist ${if (config.enableBlacklist) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
            }
            "dump" -> {
                val config = context.config
                synchronized (config) {
                    sender.sendMessage(TextComponent("${ChatColor.GREEN}Whitelist ${if (config.enableWhitelist) "ENABLED" else "${ChatColor.RED}DISABLED"} ${ChatColor.GOLD}(${config.lazyWhitelist.size} lazy record(s), ${config.whitelist.size} UUID record(s))"))
                    for (username in config.lazyWhitelist) {
                        sender.sendMessage(TextComponent("${ChatColor.AQUA}  $username"))
                    }
                    for (uuid in config.whitelist) {
                        sender.sendMessage(TextComponent("${ChatColor.AQUA}  $uuid"))
                    }
                    sender.sendMessage(TextComponent("${ChatColor.GREEN}Blacklist ${if (config.enableBlacklist) "ENABLED" else "${ChatColor.RED}DISABLED"} ${ChatColor.GOLD}(${config.lazyBlacklist.size} lazy record(s), ${config.blacklist.size} UUID record(s))"))
                    for (username in config.lazyBlacklist) {
                        sender.sendMessage(TextComponent("${ChatColor.AQUA}  $username"))
                    }
                    for (uuid in config.blacklist) {
                        sender.sendMessage(TextComponent("${ChatColor.AQUA}  $uuid"))
                    }
                }
            }
            else -> sendUsage(sender)
        }
    }
}