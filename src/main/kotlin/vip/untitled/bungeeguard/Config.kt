package vip.untitled.bungeeguard

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.logging.Logger

open class Config(val context: Plugin) {
    companion object {
        const val CONFIG = "config.yml"
        const val WHITELIST = "whitelist"
        const val BLACKLIST = "blacklist"
        const val MESSAGE = "message"
    }
    open lateinit var conf: Configuration
    protected open lateinit var whitelist: MutableSet<UUID>
    protected open lateinit var blacklist: MutableSet<UUID>
    open var message: String? = null
    protected open val dataFolder: File
        get() = context.dataFolder
    protected open val logger: Logger
        get() = context.logger
    open fun saveDefaultConfig() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val conf = File(dataFolder, CONFIG)
        if (!conf.exists()) {
            context.getResourceAsStream(CONFIG).use { `in` -> Files.copy(`in`, File(dataFolder, CONFIG).toPath()) }
        }
    }

    open fun reload() {
        load()
    }

    @Synchronized
    open fun load() {
        saveDefaultConfig()
        conf = loadConfigFromFile()
        whitelist = extractWhitelist(conf)
        blacklist = extractBlacklist(conf)
        checkWhitelistAndBlacklist(whitelist, blacklist)
        logger.info("${ChatColor.AQUA}${whitelist.size} ${ChatColor.GREEN}whitelist record(s) loaded")
        logger.info("${ChatColor.AQUA}${blacklist.size} ${ChatColor.GREEN}blacklist record(s) loaded")
        message = if (conf.contains(MESSAGE)) conf.getString(MESSAGE) else null
    }

    open fun loadConfigFromFile(): Configuration {
        return ConfigurationProvider.getProvider(YamlConfiguration::class.java).load(File(dataFolder, CONFIG))
    }

    @Synchronized
    open fun saveConfig() {
        conf.set(WHITELIST, whitelist.map { it.toString() }.toTypedArray())
        conf.set(BLACKLIST, blacklist.map { it.toString() }.toTypedArray())
        ConfigurationProvider.getProvider(YamlConfiguration::class.java).save(conf, File(dataFolder, CONFIG))
    }

    @Synchronized
    open fun inWhitelist(id: UUID): Boolean {
        return whitelist.contains(id)
    }

    @Synchronized
    open fun addWhitelistRecord(record: UUID): Boolean {
        return whitelist.add(record)
    }

    @Synchronized
    open fun removeWhitelistRecord(record: UUID): Boolean {
        return whitelist.remove(record)
    }

    @Synchronized
    open fun inBlacklist(id: UUID): Boolean {
        return blacklist.contains(id)
    }

    @Synchronized
    open fun addBlacklistRecord(record: UUID): Boolean {
        return blacklist.add(record)
    }

    @Synchronized
    open fun removeBlacklistRecord(record: UUID): Boolean {
        return blacklist.remove(record)
    }

    open fun checkWhitelistAndBlacklist(whitelist: Set<UUID>, blacklist: Set<UUID>) {
        val both = whitelist.intersect(blacklist)
        if (both.isNotEmpty()) {
            logger.warning("The following UUID(s) present(s) in both whitelist and blacklist:")
            for (uuid in both) {
                logger.warning("  ${ChatColor.AQUA}${uuid}")
            }
            logger.warning("Note that blacklist has higher priority")
        }
    }

    open fun extractWhitelist(configuration: Configuration): MutableSet<UUID> {
        return if (configuration.contains(WHITELIST)) {
            configuration.getStringList(WHITELIST).map { it -> UUID.fromString(it) }.toMutableSet()
        } else mutableSetOf()
    }

    open fun extractBlacklist(configuration: Configuration): MutableSet<UUID> {
        return if (configuration.contains(BLACKLIST)) {
            configuration.getStringList(BLACKLIST).map { it -> UUID.fromString(it) }.toMutableSet()
        } else mutableSetOf()
    }
}