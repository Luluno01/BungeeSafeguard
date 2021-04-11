package vip.untitled.bungeesafeguard.commands

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeesafeguard.Config
import vip.untitled.bungeesafeguard.MetaHolderPlugin
import vip.untitled.bungeesafeguard.UserCache
import vip.untitled.bungeesafeguard.helpers.ConcurrentTasksHelper
import vip.untitled.bungeesafeguard.helpers.ConfirmCommand
import vip.untitled.bungeesafeguard.helpers.ListDumper
import vip.untitled.bungeesafeguard.helpers.TypedJSON
import java.io.File
import java.io.IOException
import java.util.*

abstract class ListCommand(val context: MetaHolderPlugin, name: String, permission: String, vararg aliases: String): ConfirmCommand(name, permission, *aliases) {
    companion object {
        open class ConcurrentTasksHelperForDataSaving(total: Int): ConcurrentTasksHelper(total) {
            var shouldSaveConfig = false
            var shouldSaveUserCache = false
        }

        data class ListAction(
            val isXBOX: Boolean = false,
            val isLazyList: Boolean,
            val isAdd: Boolean,
            val isImport: Boolean = false
        )
    }
    protected val config: Config
        get() = context.config

    protected val userCache: UserCache
        get() = context.userCache

    /**
     * Name of this list
     */
    protected abstract val listName: String

    /**
     * Whether the list (not this command) is enabled
     */
    protected abstract val listEnabled: Boolean

    /**
     * Reference to the underlying list
     */
    protected abstract val list: MutableSet<UUID>

    /**
     * Reference to the underlying lazy list
     */
    protected abstract val lazyList: MutableSet<String>

    /**
     * Load a JSON file synchronously
     */
    open fun loadJSON(file: File): TypedJSON {
        return TypedJSON.fromString(file.readText())
    }

    /**
     * Handle each UUID element in a JSON array file asynchronously
     *
     * Type of the file should be
     * ```TypeScript
     * type UUIDArray = {
     *   uuid: string
     * }[]
     * ```
     * @param sender Command sender
     * @param file Target file
     * @param callback callback function
     */
    open fun loadUUIDsInJSONArray(sender: CommandSender, file: File, callback: (uuid: Array<String>) -> Unit) {
        context.proxy.scheduler.runAsync(context) {
            val json = try {
                loadJSON(file)
            } catch (err: IOException) {
                sender.sendMessage(TextComponent("${ChatColor.RED}\"${file.absolutePath}\" does not exists, or is unreadable"))
                return@runAsync
            }
            if (!json.json.isJsonArray) {
                sender.sendMessage(TextComponent("${ChatColor.RED}The content of \"${file.absolutePath}\" must be a JSON array"))
                return@runAsync
            }
            val uuids = arrayListOf<String>()
            for ((i, elem) in json.json.asJsonArray.withIndex()) {
                val uuid = try {
                    val obj = elem.asJsonObject
                    obj.get("uuid").asString
                } catch (err: Throwable) {
                    sender.sendMessage(TextComponent("${ChatColor.YELLOW}Element with index \"$i\" is invalid, ignored"))
                    continue
                }
                uuids.add(uuid)
            }
            callback(uuids.toTypedArray())
        }
    }

    /**
     * Add a user to the user cache
     * @param id User's ID
     * @param username Username
     */
    open fun addToUserCache(id: UUID, username: String) {
        userCache.addAndSave(id, username)
    }

    /**
     * Remove a user from the cache
     * @param id User's ID
     */
    open fun removeFromUserCache(id: UUID) {
        userCache.removeAndSave(id)
    }

    /**
     * Send usage to a command sender
     * @param sender Command sender
     */
    abstract fun sendUsage(sender: CommandSender)

    /**
     * Import UUID(s) from an existing file, merging the content into currently loaded list
     * @param sender Command sender
     * @param path Path to target file
     */
    open fun import(sender: CommandSender, path: String) {
        loadUUIDsInJSONArray(sender, File(path)) { addAsync(sender, it) }
    }

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

    open fun getConcurrentTasksHelperForDataSaving(totalTasks: Int): ConcurrentTasksHelperForDataSaving {
        return object: ConcurrentTasksHelperForDataSaving(totalTasks) {
            override fun onCompletion() {
                if (shouldSaveConfig) {
                    config.save()
                }
                if (shouldSaveUserCache) {
                    userCache.save()
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
                "import" -> {
                    if (args.size > 1) {
                        val realArgs = args.copyOfRange(1, 2)
                        possiblyDoAfterConfirmation(sender, realArgs, ListAction(isLazyList = false, isAdd = false, isImport = true)) { import(sender, args[1]) }
                    } else {
                        sendUsage(sender)
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
                "list", "ls", "show", "dump" -> {
                    val config = context.config
                    synchronized(config) {
                        ListDumper.printListStatus(sender, listName.capitalize(), listEnabled)
                        ListDumper.printListsContent(sender, lazyList, list, userCache)
                    }
                }
                else -> sendUsage(sender)
            }
        }
    }
}