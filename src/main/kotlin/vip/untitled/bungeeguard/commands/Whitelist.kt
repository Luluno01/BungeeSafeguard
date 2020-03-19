package vip.untitled.bungeeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import vip.untitled.bungeeguard.Config
import vip.untitled.bungeeguard.ConfigHolderPlugin

open class Whitelist(val context: ConfigHolderPlugin): Command("whitelist", "bungeeguard.whitelist", "wlist") {
    protected val config: Config
        get() = context.config
    open fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /whitelist <add/remove> <player ...>"))
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
        } else {
            when (args[0]) {
                "add" -> {
                    if (args.size > 1) {
                        for (usernameOrUUID in args.slice(1 until args.size)) {
                            UserUUIDHelper.getUUIDFromString(context, usernameOrUUID) { err, uuid ->
                                if (err == null) {
                                    assert(uuid != null) { "Both error and UUID are null!" }
                                    synchronized (config) {
                                        if (config.inBlacklist(uuid!!)) {
                                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already blacklisted, whose priority is higher than whitelist"))
                                        }
                                        config.addWhitelistRecord(uuid)
                                        sender.sendMessage(TextComponent("${ChatColor.GREEN}${usernameOrUUID} added to whitelist"))
                                        config.saveConfig()
                                    }
                                } else {
                                    sender.sendMessage(TextComponent("${ChatColor.RED} failed to whitelist $usernameOrUUID: $err"))
                                    context.logger.warning("Failed to whitelist $usernameOrUUID:")
                                    err.printStackTrace()
                                }
                            }
                        }
                    } else {
                        sendUsage(sender)
                    }
                }
                "remove" -> {
                    if (args.size > 1) {
                        for (usernameOrUUID in args.slice(1 until args.size)) {
                            UserUUIDHelper.getUUIDFromString(context, usernameOrUUID) { err, uuid ->
                                if (err == null) {
                                    assert(uuid != null) { "Both error and UUID are null!" }
                                    synchronized (config) {
                                        if (config.inWhitelist(uuid!!)) {
                                            config.removeWhitelistRecord(uuid)
                                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} removed from whitelist"))
                                            config.saveConfig()
                                        } else {
                                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is not in whitelist"))
                                        }
                                    }
                                } else {
                                    sender.sendMessage(TextComponent("${ChatColor.RED} failed to remove $usernameOrUUID from whitelist: $err"))
                                    context.logger.warning("Failed to remove $usernameOrUUID from whitelist:")
                                    err.printStackTrace()
                                }
                            }
                        }
                    } else {
                        sendUsage(sender)
                    }
                }
                else -> sendUsage(sender)
            }
        }
    }
}