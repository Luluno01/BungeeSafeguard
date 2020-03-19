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
        when (args[0]) {
            "reload" -> context.proxy.scheduler.runAsync(context) {
                try {
                    context.config.reload()
                    sender.sendMessage(TextComponent("${ChatColor.GREEN}BungeeGuard reloaded"))
                } catch (e: Throwable) {
                    sender.sendMessage(TextComponent("${ChatColor.RED}Failed to reload: $e"))
                }
            }
            else -> sendUsage(sender)
        }
    }
}