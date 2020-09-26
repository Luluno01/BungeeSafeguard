package vip.untitled.bungeesafeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeesafeguard.Config
import vip.untitled.bungeesafeguard.ConfigHolderPlugin
import vip.untitled.bungeesafeguard.helpers.ConcurrentTasksHelper
import vip.untitled.bungeesafeguard.helpers.ConfirmCommand

abstract class ListCommand(val context: ConfigHolderPlugin, name: String, permission: String, vararg aliases: String): ConfirmCommand(name, permission, *aliases) {
    companion object {
        open class ConcurrentTasksHelperForConfigSaving(total: Int): ConcurrentTasksHelper(total) {
            var shouldSaveConfig = false
        }

        data class ListAction(val isXBOX: Boolean = false, val isLazyList: Boolean, val isAdd: Boolean)
    }
    protected val config: Config
        get() = context.config

    /**
     * Send usage to a command sender
     * @param sender Command sender
     */
    abstract fun sendUsage(sender: CommandSender)

    /**
     * Add UUID(s) or username(s) to the list asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    abstract fun addAsync(sender: CommandSender, args: Array<out String>)

    /**
     * Add UUID(s) converted from XUID(s) or XBOX Gamertag(s) to the list asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    abstract fun xAddAsync(sender: CommandSender, args: Array<out String>)

    /**
     * Lazy-add UUID(s) or username(s) to the list synchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    abstract fun lazyAdd(sender: CommandSender, args: Array<out String>)

    /**
     * Remove UUID(s) or username(s) from the list asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    abstract fun removeAsync(sender: CommandSender, args: Array<out String>)

    /**
     * Remove UUID(s) converted from XUID(s) or XBOX Gamertag(s) from the list asynchronously
     * @param sender Command sender
     * @param args Array of UUID(s) converted from XUID(s) or XBOX Gamertag(s) (can be a mixed array)
     */
    abstract fun xRemoveAsync(sender: CommandSender, args: Array<out String>)

    /**
     * Lazy-remove UUID(s) or username(s) from the list synchronously
     * @param sender Command sender
     * @param args Array of UUID(s) or username(s) (can be a mixed array)
     */
    abstract fun lazyRemove(sender: CommandSender, args: Array<out String>)

    /**
     * Turn on the list
     * @param sender Command sender
     */
    abstract fun on(sender: CommandSender)

    /**
     * Turn off the list
     * @param sender Command sender
     */
    abstract fun off(sender: CommandSender)

    open fun getConcurrentTasksHelperForConfigSaving(totalTasks: Int): ConcurrentTasksHelperForConfigSaving {
        return object: ConcurrentTasksHelperForConfigSaving(totalTasks) {
            override fun onCompletion() {
                if (shouldSaveConfig) {
                    synchronized (config) {
                        config.save()
                    }
                }
            }
        }
    }

    /**
     * Send confirm message to the command sender
     */
    abstract fun sendConfirmMessage(sender: CommandSender, args: Array<out String>, action: ListAction)

    protected open fun possiblyDoAfterConfirmation(sender: CommandSender, args: Array<out String>, action: ListAction, callback: () -> Unit) {
        if (config.confirm) {
            sendConfirmMessage(sender, args, action)
            confirm(sender, callback)
        } else {
            callback()  // Do it now
        }
    }

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
        } else {
            when (args[0]) {
                "confirm" -> {
                    if (!confirmed(sender)) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Nothing to confirm, it might have expired"))
                    }
                }
                "add" -> {
                    if (args.size > 1) {
                        val realArgs = args.copyOfRange(1, args.size)
                        possiblyDoAfterConfirmation(sender, realArgs, ListAction(isLazyList = false, isAdd = true)) { addAsync(sender, realArgs) }
                    } else {
                        sendUsage(sender)
                    }
                }
                "x-add", "xadd" -> {
                    if (args.size > 1) {
                        val realArgs = args.copyOfRange(1, args.size)
                        possiblyDoAfterConfirmation(sender, realArgs, ListAction(isXBOX = true, isLazyList = false, isAdd = true)) { xAddAsync(sender, realArgs) }
                    } else {
                        sendUsage(sender)
                    }
                }
                "lazy-add", "lazyadd", "ladd" -> {
                    if (args.size > 1) {
                        val realArgs = args.copyOfRange(1, args.size)
                        possiblyDoAfterConfirmation(sender, realArgs, ListAction(isLazyList = true, isAdd = true)) { lazyAdd(sender, realArgs) }
                    } else {
                        sendUsage(sender)
                    }
                }
                "rm", "remove" -> {
                    if (args.size > 1) {
                        val realArgs = args.copyOfRange(1, args.size)
                        possiblyDoAfterConfirmation(sender, realArgs, ListAction(isLazyList = false, isAdd = false)) { removeAsync(sender, realArgs) }
                    } else {
                        sendUsage(sender)
                    }
                }
                "x-rm", "xrm",
                "x-remove", "xremove" -> {
                    if (args.size > 1) {
                        val realArgs = args.copyOfRange(1, args.size)
                        possiblyDoAfterConfirmation(sender, realArgs, ListAction(isXBOX = true, isLazyList = false, isAdd = false)) { xRemoveAsync(sender, realArgs) }
                    } else {
                        sendUsage(sender)
                    }
                }
                "lazy-remove", "lazyremove", "lremove", "lrm" -> {
                    if (args.size > 1) {
                        val realArgs = args.copyOfRange(1, args.size)
                        possiblyDoAfterConfirmation(sender, realArgs, ListAction(isLazyList = true, isAdd = false)) { lazyRemove(sender, realArgs) }
                    } else {
                        sendUsage(sender)
                    }
                }
                "on" -> {
                    on(sender)
                }
                "off" -> {
                    off(sender)
                }
                else -> sendUsage(sender)
            }
        }
    }
}