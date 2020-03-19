package vip.untitled.bungeeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import vip.untitled.bungeeguard.Config
import vip.untitled.bungeeguard.ConfigHolderPlugin

open class Blacklist(val context: ConfigHolderPlugin): Command("blacklist", "bungeeguard.blacklist", "blist") {
    protected val config: Config
        get() = context.config
    open fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /blacklist <add/remove> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Or:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /blacklist <on/off>"))
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
                                        if (config.inWhitelist(uuid!!)) {
                                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already whitelisted, whose priority is lower than blacklist"))
                                        }
                                        config.addBlacklistRecord(uuid)
                                        sender.sendMessage(TextComponent("${ChatColor.AQUA}${usernameOrUUID} added to blacklist"))
                                        config.save()
                                    }
                                } else {
                                    sender.sendMessage(TextComponent("${ChatColor.RED} failed to blacklist $usernameOrUUID: $err"))
                                    context.logger.warning("Failed to blacklist $usernameOrUUID:")
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
                                        if (config.inBlacklist(uuid!!)) {
                                            config.removeBlacklistRecord(uuid)
                                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} removed from blacklist"))
                                            config.save()
                                        } else {
                                            sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is not in blacklist"))
                                        }
                                    }
                                } else {
                                    sender.sendMessage(TextComponent("${ChatColor.RED} failed to remove $usernameOrUUID from blacklist: $err"))
                                    context.logger.warning("Failed to remove $usernameOrUUID from blacklist:")
                                    err.printStackTrace()
                                }
                            }
                        }
                    } else {
                        sendUsage(sender)
                    }
                }
                "on" -> {
                    synchronized (config) {
                        config.setBlacklistEnabled(true)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}Blacklist ENABLED"))
                        config.save()
                    }
                }
                "off" -> {
                    synchronized (config) {
                        config.setBlacklistEnabled(false)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}Blacklist ${ChatColor.RED}DISABLED"))
                        config.save()
                    }
                }
                else -> sendUsage(sender)
            }
        }
    }
}