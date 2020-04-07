package vip.untitled.bungeeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeeguard.ConfigHolderPlugin
import vip.untitled.bungeeguard.helpers.UserUUIDHelper
import java.util.*

open class Whitelist(context: ConfigHolderPlugin): ListCommand(context, "whitelist", "bungeeguard.whitelist", "wlist") {
    override fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /whitelist <add/remove/rm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Or:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /whitelist <lazy-add/lazy-remove/lazyadd/ladd/lazyremove/lremove/lrm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Or:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /whitelist <on/off>"))
    }

    /**
     * Add UUID(s) or username(s) to whitelist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    override fun addAsync(sender: CommandSender, args: Array<out String>) {
        val concurrentTasksHelper = getConcurrentTasksHelperForConfigSaving(args.size)
        for (usernameOrUUID in args) {
            UserUUIDHelper.getUUIDFromString(context, usernameOrUUID) { err, uuid ->
                try {
                    if (err == null) {
                        assert(uuid != null) { "Both error and UUID are null!" }
                        synchronized(config) {
                            if (config.inBlacklist(uuid!!)) {
                                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already blacklisted, whose priority is higher than whitelist"))
                            }
                            if (config.inWhitelist(uuid)) {
                                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already whitelisted"))
                            } else {
                                config.addWhitelistRecord(uuid)
                                sender.sendMessage(TextComponent("${ChatColor.GREEN}${usernameOrUUID} added to whitelist"))
                                synchronized (concurrentTasksHelper) {
                                    concurrentTasksHelper.shouldSaveConfig = true
                                }
                            }
                        }
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.RED} failed to whitelist $usernameOrUUID: $err"))
                        context.logger.warning("Failed to whitelist $usernameOrUUID:")
                        err.printStackTrace()
                    }
                } finally {
                    concurrentTasksHelper.notifyCompletion()
                }
            }
        }
    }

    /**
     * Remove UUID(s) or username(s) from whitelist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    override fun removeAsync(sender: CommandSender, args: Array<out String>) {
        val concurrentTasksHelper = getConcurrentTasksHelperForConfigSaving(args.size)
        for (usernameOrUUID in args) {
            UserUUIDHelper.getUUIDFromString(context, usernameOrUUID) { err, uuid ->
                try {
                    if (err == null) {
                        assert(uuid != null) { "Both error and UUID are null!" }
                        synchronized (config) {
                            if (config.inWhitelist(uuid!!)) {
                                config.removeWhitelistRecord(uuid)
                                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} removed from whitelist"))
                                concurrentTasksHelper.shouldSaveConfig = true
                            } else {
                                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is not in whitelist"))
                            }
                        }
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.RED} failed to remove $usernameOrUUID from whitelist: $err"))
                        context.logger.warning("Failed to remove $usernameOrUUID from whitelist:")
                        err.printStackTrace()
                    }
                } finally {
                    concurrentTasksHelper.notifyCompletion()
                }
            }
        }
    }

    override fun lazyAdd(sender: CommandSender, args: Array<out String>) {
        var shouldSaveConfig = false
        for (usernameOrUUID in args) {
            try {
                val uuid = UUID.fromString(usernameOrUUID)
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is a UUID and will be added to whitelist"))
                synchronized (config) {
                    if (config.inBlacklist(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${uuid} is already blacklisted, whose priority is higher than whitelist"))
                    }
                    if (config.inWhitelist(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already whitelisted"))
                    } else {
                        config.addWhitelistRecord(uuid)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}${uuid} added to whitelist"))
                        shouldSaveConfig = true
                    }
                }
            } catch (e: IllegalArgumentException) {
                synchronized (config) {
                    if (config.inLazyBlacklist(usernameOrUUID)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already lazy-blacklisted, whose priority is higher than whitelist"))
                    }
                    if (config.inLazyWhitelist(usernameOrUUID)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already lazy-whitelisted"))
                    } else {
                        config.addLazyWhitelistRecord(usernameOrUUID)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}${usernameOrUUID} added to lazy-whitelist"))
                        shouldSaveConfig = true
                    }
                }
            }
        }
        if (shouldSaveConfig) {
            synchronized (config) {
                config.save()
            }
        }
    }

    override fun lazyRemove(sender: CommandSender, args: Array<out String>) {
        var shouldSaveConfig = false
        for (usernameOrUUID in args) {
            try {
                val uuid = UUID.fromString(usernameOrUUID)
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is a UUID and will be removed from whitelist"))
                synchronized (config) {
                    if (config.inWhitelist(uuid)) {
                        config.removeWhitelistRecord(uuid)
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} removed from whitelist"))
                        shouldSaveConfig = true
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is not in whitelist"))
                    }
                }
            } catch (e: IllegalArgumentException) {
                synchronized (config) {
                    if (config.inLazyWhitelist(usernameOrUUID)) {
                        config.removeLazyWhitelistRecord(usernameOrUUID)
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} removed from lazy-whitelist"))
                        shouldSaveConfig = true
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is not in lazy-whitelist"))
                    }
                }
            }
        }
        if (shouldSaveConfig) {
            synchronized (config) {
                config.save()
            }
        }
    }

    /**
     * Turn on whitelist
     * @param sender Command sender
     */
    override fun on(sender: CommandSender) {
        synchronized (config) {
            config.setWhitelistEnabled(true)
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Whitelist ENABLED"))
            config.save()
        }
    }

    /**
     * Turn off whitelist
     * @param sender Command sender
     */
    override fun off(sender: CommandSender) {
        synchronized (config) {
            config.setWhitelistEnabled(false)
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Whitelist ${ChatColor.RED}DISABLED"))
            config.save()
        }
    }
}