package vip.untitled.bungeesafeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeesafeguard.ConfigHolderPlugin
import vip.untitled.bungeesafeguard.helpers.UserNotFoundException
import vip.untitled.bungeesafeguard.helpers.UserUUIDHelper
import java.util.*

open class Blacklist(context: ConfigHolderPlugin): ListCommand(context, "blacklist", "bungeesafeguard.blacklist", "blist") {
    override fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For normal Mojang players: /blacklist <add/remove/rm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For XBOX Live players: /blacklist <x-add/xadd/x-remove/x-remove/x-rm/xrm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For both Mojang and XBOX players: /blacklist <lazy-add/lazy-remove/lazyadd/ladd/lazyremove/lremove/lrm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /blacklist <on/off>"))
    }

    /**
     * Add UUID(s) or username(s) to blacklist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    override fun addAsync(sender: CommandSender, args: Array<out String>) {
        addAsync(sender, args, false)
    }

    /**
     * Add UUID(s) converted from XUID(s) or XBOX Gamertag(s) to blacklist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    override fun xAddAsync(sender: CommandSender, args: Array<out String>) {
        addAsync(sender, args, true)
    }

    protected fun addAsync(sender: CommandSender, args: Array<out String>, xbox: Boolean) {
        var lastSyncError: Throwable? = null
        val concurrentTasksHelper = getConcurrentTasksHelperForConfigSaving(args.size)
        for (usernameOrUUID in args) {
            val onUUID = { err: Throwable?, uuid: UUID? ->
                try {
                    when (err) {
                        null -> {
                            assert(uuid != null) { "Both error and UUID are null!" }
                            synchronized(config) {
                                if (config.inWhitelist(uuid!!)) {
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is already whitelisted, whose priority is lower than blacklist"))
                                }
                                if (config.inBlacklist(uuid)) {
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is already blacklisted"))
                                } else {
                                    config.addBlacklistRecord(uuid)
                                    sender.sendMessage(TextComponent("${ChatColor.AQUA}${usernameOrUUID} ${ChatColor.YELLOW}($uuid) ${ChatColor.AQUA}added to blacklist"))
                                    synchronized (concurrentTasksHelper) {
                                        concurrentTasksHelper.shouldSaveConfig = true
                                    }
                                }
                            }
                        }
                        is UserNotFoundException -> sender.sendMessage(TextComponent("${ChatColor.RED}User $usernameOrUUID is not found and therefore cannot be blacklisted"))
                        else -> {
                            sender.sendMessage(TextComponent("${ChatColor.RED}Failed to blacklist $usernameOrUUID: $err"))
                            context.logger.warning("Failed to blacklist $usernameOrUUID:")
                            err.printStackTrace()
                        }
                    }
                } finally {
                    concurrentTasksHelper.notifyCompletion()
                }
            }
            try {
                if (xbox) UserUUIDHelper.getUUIDFromXBOXTag(context, usernameOrUUID, onUUID)
                else UserUUIDHelper.getUUIDFromString(context, usernameOrUUID, onUUID)
            } catch (e: Throwable) {
                lastSyncError = e
                concurrentTasksHelper.notifyCompletion()
            }
        }
        if (lastSyncError != null) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}Some error occurred, check the console for more details"))
            context.logger.warning("Last error:")
            lastSyncError.printStackTrace()
        }
    }

    /**
     * Remove UUID(s) or username(s) from blacklist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    override fun removeAsync(sender: CommandSender, args: Array<out String>) {
        removeAsync(sender, args, false)
    }

    /**
     * Remove UUID(s) converted from XUID(s) or XBOX Gamertag(s) from blacklist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) converted from XUID(s) or XBOX Gamertag(s) (can be a mixed array)
     */
    override fun xRemoveAsync(sender: CommandSender, args: Array<out String>) {
        removeAsync(sender, args, true)
    }

    protected fun removeAsync(sender: CommandSender, args: Array<out String>, xbox: Boolean) {
        var lastSyncError: Throwable? = null
        val concurrentTasksHelper = getConcurrentTasksHelperForConfigSaving(args.size)
        for (usernameOrUUID in args) {
            val onUUID = { err: Throwable?, uuid: UUID? ->
                try {
                    when (err) {
                        null -> {
                            assert(uuid != null) { "Both error and UUID are null!" }
                            synchronized (config) {
                                if (config.inBlacklist(uuid!!)) {
                                    config.removeBlacklistRecord(uuid)
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}removed from blacklist"))
                                    concurrentTasksHelper.shouldSaveConfig = true
                                } else {
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is not in blacklist"))
                                }
                            }
                        }
                        is UserNotFoundException -> sender.sendMessage(TextComponent("${ChatColor.RED}User $usernameOrUUID is not found and therefore cannot be removed from blacklist"))
                        else -> {
                            sender.sendMessage(TextComponent("${ChatColor.RED}Failed to remove $usernameOrUUID from blacklist: $err"))
                            context.logger.warning("Failed to remove $usernameOrUUID from blacklist:")
                            err.printStackTrace()
                        }
                    }
                } finally {
                    concurrentTasksHelper.notifyCompletion()
                }
            }
            try {
                if (xbox) UserUUIDHelper.getUUIDFromXBOXTag(context, usernameOrUUID, onUUID)
                else UserUUIDHelper.getUUIDFromString(context, usernameOrUUID, onUUID)
            } catch (e: Throwable) {
                lastSyncError = e
                concurrentTasksHelper.notifyCompletion()
            }
        }
        if (lastSyncError != null) {
            sender.sendMessage(TextComponent("${ChatColor.YELLOW}Some error occurred, check the console for more details"))
            context.logger.warning("Last error:")
            lastSyncError.printStackTrace()
        }
    }

    override fun lazyAdd(sender: CommandSender, args: Array<out String>) {
        var shouldSaveConfig = false
        for (usernameOrUUID in args) {
            try {
                val uuid = UUID.fromString(usernameOrUUID)
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is a UUID and will be added to blacklist"))
                synchronized (config) {
                    if (config.inWhitelist(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${uuid} is already whitelisted, whose priority is lower than blacklist"))
                    }
                    if (config.inBlacklist(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already blacklisted"))
                    } else {
                        config.addBlacklistRecord(uuid)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}${uuid} added to blacklist"))
                        shouldSaveConfig = true
                    }
                }
            } catch (e: IllegalArgumentException) {
                synchronized (config) {
                    if (config.inLazyWhitelist(usernameOrUUID)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already lazy-whitelisted, whose priority is lower than blacklist"))
                    }
                    if (config.inLazyBlacklist(usernameOrUUID)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already lazy-blacklisted"))
                    } else {
                        config.addLazyBlacklistRecord(usernameOrUUID)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}${usernameOrUUID} added to lazy-blacklist"))
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
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is a UUID and will be removed from blacklist"))
                synchronized (config) {
                    if (config.inBlacklist(uuid)) {
                        config.removeBlacklistRecord(uuid)
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} removed from blacklist"))
                        shouldSaveConfig = true
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is not in blacklist"))
                    }
                }
            } catch (e: IllegalArgumentException) {
                synchronized (config) {
                    if (config.inLazyBlacklist(usernameOrUUID)) {
                        config.removeLazyBlacklistRecord(usernameOrUUID)
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} removed from lazy-blacklist"))
                        shouldSaveConfig = true
                    } else {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is not in lazy-blacklist"))
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
     * Turn on blacklist
     * @param sender Command sender
     */
    override fun on(sender: CommandSender) {
        synchronized (config) {
            config.setBlacklistEnabled(true)
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Blacklist ENABLED"))
            config.save()
        }
    }

    /**
     * Turn off blacklist
     * @param sender Command sender
     */
    override fun off(sender: CommandSender) {
        synchronized (config) {
            config.setBlacklistEnabled(false)
            sender.sendMessage(TextComponent("${ChatColor.GREEN}Blacklist ${ChatColor.RED}DISABLED"))
            config.save()
        }
    }
}