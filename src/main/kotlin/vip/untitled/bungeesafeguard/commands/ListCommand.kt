package vip.untitled.bungeesafeguard.commands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import vip.untitled.bungeesafeguard.Config
import vip.untitled.bungeesafeguard.ConfigHolderPlugin
import vip.untitled.bungeesafeguard.helpers.ConcurrentTasksHelper

abstract class ListCommand(val context: ConfigHolderPlugin, name: String, permission: String, vararg aliases: String): Command(name, permission, *aliases) {
    companion object {
        open class ConcurrentTasksHelperForConfigSaving(total: Int): ConcurrentTasksHelper(total) {
            var shouldSaveConfig = false
        }
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

    override fun execute(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sendUsage(sender)
        } else {
            when (args[0]) {
                "add" -> {
                    if (args.size > 1) {
                        addAsync(sender, args.copyOfRange(1, args.size))
                    } else {
                        sendUsage(sender)
                    }
                }
                "lazy-add", "lazyadd", "ladd" -> {
                    if (args.size > 1) {
                        lazyAdd(sender, args.copyOfRange(1, args.size))
                    } else {
                        sendUsage(sender)
                    }
                }
                "rm", "remove" -> {
                    if (args.size > 1) {
                        removeAsync(sender, args.copyOfRange(1, args.size))
                    } else {
                        sendUsage(sender)
                    }
                }
                "lazy-remove", "lazyremove", "lremove", "lrm" -> {
                    if (args.size > 1) {
                        lazyRemove(sender, args.copyOfRange(1, args.size))
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