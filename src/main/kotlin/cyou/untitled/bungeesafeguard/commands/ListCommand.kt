package cyou.untitled.bungeesafeguard.commands

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.commands.subcommands.list.Base
import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import io.ktor.client.*

abstract class ListCommand(
    val context: BungeeSafeguard,
    protected val client: HttpClient,
    protected val listMgr: ListManager,
    protected val list: UUIDList,
    name: String, permission: String, vararg aliases: String) : ConfirmCommand(
    name,
    permission,
    *aliases
) {
    companion object {
        @Suppress("unused")
        enum class SubcommandName(val cmdName: String, vararg val aliases: String) {
            IMPORT("import"),
            ADD("add"),
            X_ADD("x-add", "xadd"),
            LAZY_ADD("lazy-add", "lazyadd", "ladd"),
            REMOVE("remove", "rm"),
            X_REMOVE("x-remove", "xremove", "x-rm", "xrm"),
            LAZY_REMOVE("lazy-remove", "lazyremove", "lremove", "lrm"),
            ON("on"),
            OFF("off"),
            LIST("list", "ls", "show", "dump");

            companion object {
                /**
                 * Get `SubcommandName` from its command name
                 */
                fun fromCmdName(name: String): SubcommandName? {
                    return values().find { it.cmdName == name }
                }
            }

            override fun toString(): String = cmdName
        }
    }

    /**
     * Get the instance of a subcommand by name
     *
     * @param name the name of the subcommand
     */
    abstract fun getSubcommand(name: SubcommandName): Base?

    /**
     * Get the instance of a subcommand by name
     *
     * @param name the name of the subcommand
     */
    abstract fun getSubcommand(name: String): Base?
}