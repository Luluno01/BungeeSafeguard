package cyou.untitled.bungeesafeguard.commands.subcommands

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin

@Suppress("MemberVisibilityCanBePrivate")
open class SubcommandRegistry(val context: Plugin, protected val usage: UsageSender) {
    companion object {
        interface UsageSender {
            fun sendUsage(sender: CommandSender)
        }
    }

    private val subcommands = mutableListOf<Subcommand>()
    private val subcommandMap = mutableMapOf<String, Subcommand>()

    open fun registerSubcommand(subcommand: Subcommand): SubcommandRegistry {
        assertUnused(subcommand.name)
        for (alias in subcommand.aliases) {
            assertUnused(alias)
        }
        subcommands.add(subcommand)
        subcommandMap[subcommand.name] = subcommand
        for (alias in subcommand.aliases) {
            subcommandMap[alias] = subcommand
        }
        return this
    }

    open fun registerSubcommand(vararg subcommand: Subcommand): SubcommandRegistry {
        for (cmd in subcommand) {
            registerSubcommand(cmd)
        }
        return this
    }

    protected fun assertUnused(name: String) {
        assert(!subcommandMap.containsKey(name)) { "Subcommand $name is already registered" }
    }

    /**
     * Get the instance of a subcommand by name
     *
     * @param name the name of the subcommand
     */
    open fun getSubcommand(name: String): Subcommand? = subcommandMap[name]

    /**
     * Get the instance of a subcommand by the first argument
     * @param sender
     * @param args
     */
    open fun getSubcommand(sender: CommandSender, args: Array<out String>): Subcommand? {
        if (args.isEmpty()) {
            return null.also { usage.sendUsage(sender) }
        } else {
            return subcommandMap[args[0]] ?: return null.also { usage.sendUsage(sender) }
        }
    }

    /**
     * Get a **copy** of list of registered subcommands
     */
    open fun getSubcommands(): List<Subcommand> {
        return subcommands.toList()
    }
}