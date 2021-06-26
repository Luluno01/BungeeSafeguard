package cyou.untitled.bungeesafeguard.storage

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import java.io.File

/**
 * The default backend that uses the config file to store the lists
 */
open class ConfigBackend(context: Plugin, configFile: File): YAMLBackend(context, configFile) {
    override suspend fun onReloadConfigFile(newConfig: File, commandSender: CommandSender?) {
        close(commandSender)
        init(newConfig, commandSender)
    }

    override fun toString(): String = "ConfigBackend(\"${file?.path ?: "<null>"}\")"
}