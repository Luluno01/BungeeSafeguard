package vip.untitled.bungeesafeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeesafeguard.ConfigHolderPlugin
import vip.untitled.bungeesafeguard.helpers.UserNotFoundException
import vip.untitled.bungeesafeguard.helpers.UserUUIDHelper
import java.util.*

open class Whitelist(context: ConfigHolderPlugin): ListCommand(context, "whitelist", "bungeesafeguard.whitelist", "wlist") {
    override fun sendUsage(sender: CommandSender) {
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Usage:"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For normal Mojang players: /whitelist <add/remove/rm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For XBOX Live players: /whitelist <x-add/xadd/x-remove/xremove/x-rm/xrm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  For both Mojang and XBOX players: /whitelist <lazy-add/lazy-remove/lazyadd/ladd/lazyremove/lremove/lrm> <player ...>"))
        sender.sendMessage(TextComponent("${ChatColor.YELLOW}  /whitelist <on/off>"))
    }

    /**
     * Add UUID(s) or username(s) to whitelist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    override fun addAsync(sender: CommandSender, args: Array<out String>) {
        addAsync(sender, args, false)
    }

    /**
     * Add UUID(s) converted from XUID(s) or XBOX Gamertag(s) to whitelist asynchronously
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
                                if (config.inBlacklist(uuid!!)) {
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is already blacklisted, whose priority is higher than whitelist"))
                                }
                                if (config.inWhitelist(uuid)) {
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is already whitelisted"))
                                } else {
                                    config.addWhitelistRecord(uuid)
                                    sender.sendMessage(TextComponent("${ChatColor.GREEN}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.GREEN}added to whitelist"))
                                    synchronized (concurrentTasksHelper) {
                                        concurrentTasksHelper.shouldSaveConfig = true
                                    }
                                }
                            }
                        }
                        is UserNotFoundException -> sender.sendMessage(TextComponent("${ChatColor.RED}User $usernameOrUUID ${ChatColor.AQUA}($uuid) ${ChatColor.RED}is not found and therefore cannot be whitelisted"))
                        else -> {
                            sender.sendMessage(TextComponent("${ChatColor.RED}Failed to whitelist $usernameOrUUID: $err"))
                            context.logger.warning("Failed to whitelist $usernameOrUUID:")
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
     * Remove UUID(s) or username(s) from whitelist asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    override fun removeAsync(sender: CommandSender, args: Array<out String>) {
        removeAsync(sender, args, false)
    }

    /**
     * Remove UUID(s) converted from XUID(s) or XBOX Gamertag(s) from whitelist asynchronously
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
                                if (config.inWhitelist(uuid!!)) {
                                    config.removeWhitelistRecord(uuid)
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}removed from whitelist"))
                                    concurrentTasksHelper.shouldSaveConfig = true
                                } else {
                                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} ${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}is not in whitelist"))
                                }
                            }
                        }
                        is UserNotFoundException -> sender.sendMessage(TextComponent("${ChatColor.RED}User $usernameOrUUID ${ChatColor.AQUA}($uuid) ${ChatColor.RED}is not found and therefore cannot be removed from whitelist"))
                        else -> {
                            sender.sendMessage(TextComponent("${ChatColor.RED}Failed to remove $usernameOrUUID from whitelist: $err"))
                            context.logger.warning("Failed to remove $usernameOrUUID from whitelist:")
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
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is a UUID and will be added to whitelist"))
                synchronized (config) {
                    if (config.inBlacklist(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${uuid} is already blacklisted, whose priority is higher than whitelist"))
                    }
                    if (config.inWhitelist(uuid)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}${usernameOrUUID} is already whitelisted"))
                    } else {
                        config.addWhitelistRecord(uuid)
                        sender.sendMessage(TextComponent("${ChatColor.GREEN}${ChatColor.AQUA}($uuid) ${ChatColor.YELLOW}${uuid} added to whitelist"))
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