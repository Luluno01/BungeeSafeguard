package cyou.untitled.bungeesafeguard.storage

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.helpers.ListChecker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.config.Configuration
import net.md_5.bungee.config.ConfigurationProvider
import net.md_5.bungee.config.YamlConfiguration
import java.io.File
import java.util.*

/**
 * The backend that uses a YAML file to store the lists in human-friendly format
 * (i.e., the legacy format in older versions)
 */
@Suppress("BlockingMethodInNonBlockingContext", "MemberVisibilityCanBePrivate")
open class YAMLBackend(context: Plugin, initFile: File? = null): Backend(context) {
    companion object {
        /* YAML entries */
        const val WHITELIST = "whitelist"
        const val LAZY_WHITELIST = "lazy-whitelist"
        const val BLACKLIST = "blacklist"
        const val LAZY_BLACKLIST = "lazy-blacklist"

        val pathTranslations = mapOf(
            Pair("whitelist.main", WHITELIST),
            Pair("whitelist.lazy", LAZY_WHITELIST),
            Pair("blacklist.main", BLACKLIST),
            Pair("blacklist.lazy", LAZY_BLACKLIST)
        )

        /**
         * Translate the path to legacy config entry
         *
         * @param path the list path
         */
        fun translatePath(path: Array<String>): String {
            assert(path.isNotEmpty()) { "Empty path" }
            val pathString = path.joinToString(".")
            return pathTranslations[pathString] ?: error("Invalid path \"$pathString\"")
        }

        protected val yamlConfigProvider = ConfigurationProvider.getProvider(YamlConfiguration::class.java)!!
    }

    @Suppress("PropertyName")
    var file: File? = initFile
        protected set

    val name = "YAMLBackend-$id"

    protected val lock = Mutex()

    open suspend fun init(file: File, commandSender: CommandSender?) {
        lock.withLock {
            assert(this.file == null) { "Concurrent initialization, or reinitialize without first closing" }
            this.file = file
            FileManager.withFile(file.path, name) {
                // If it does not throw, assume the file is OK
                yamlConfigProvider.load(it)
            }
            commandSender?.sendMessage(TextComponent("${ChatColor.GREEN}YAMLBackend is using \"${file.path}\" as the underlying storage file"))
        }
        // Sanity check
        val bsg = try {
            BungeeSafeguard.getPlugin()
        } catch (err: UninitializedPropertyAccessException) { return }
        val listMgr = bsg.listMgr
        ListChecker.checkLists(bsg, null, listMgr, { get(it.lazyPath) }, { it.lazyName })
        ListChecker.checkLists(bsg, null, listMgr, { get(it.path) }, { it.name })
    }

    override suspend fun init(commandSender: CommandSender?) {
        val file = file ?: error("This backend was not provided with the underlying file when initialized")
        init(file, commandSender)
    }

    override suspend fun close(commandSender: CommandSender?) {
        lock.withLock {
            file = null
        }
    }

    override suspend fun reload(commandSender: CommandSender?) {
        lock.withLock {
            // Do nothing as we don't cache the data here
        }
    }

    protected suspend fun <T>withEntryAndFile(path: Array<String>, block: suspend (String, File) -> T): T {
        lock.withLock {
            // First make sure the path is valid
            val entry = translatePath(path)
            return FileManager.withFile(file!!.path, name) { block(entry, it) }
        }
    }

    protected suspend fun <T>withEntryAndConfigFile(path: Array<String>, block: suspend (String, Configuration) -> T): T {
        return withEntryAndFile(path) { entry, _ ->
            val conf = yamlConfigProvider.load(file!!)
            return@withEntryAndFile block(entry, conf)
        }
    }

    protected suspend fun <T>withConfigFile(block: suspend (Configuration) -> T): T {
        lock.withLock {
            return FileManager.withFile(file!!.path, name) {
                val conf = yamlConfigProvider.load(file!!)
                return@withFile block(conf)
            }
        }
    }

    override suspend fun add(path: Array<String>, rawRecord: String): Boolean {
        return withEntryAndConfigFile(path) { entry, conf ->
            val records = conf.getStringList(entry).toMutableSet()
            if (records.add(rawRecord)) {
                conf.set(entry, records.toTypedArray())
                yamlConfigProvider.save(conf, file!!)
                true
            } else {
                false
            }
        }
    }

    override suspend fun remove(path: Array<String>, rawRecord: String): Boolean {
        return withEntryAndConfigFile(path) { entry, conf ->
            val records = conf.getStringList(entry).toMutableSet()
            if (records.remove(rawRecord)) {
                conf.set(entry, records.toTypedArray())
                yamlConfigProvider.save(conf, file!!)
                true
            } else {
                false
            }
        }
    }

    override suspend fun has(path: Array<String>, rawRecord: String): Boolean {
        return withEntryAndConfigFile(path) { entry, conf ->
            conf.getStringList(entry).contains(rawRecord)
        }
    }

    override suspend fun getSize(path: Array<String>): Int {
        return get(path).size
    }

    override suspend fun get(path: Array<String>): Set<String> {
        return withEntryAndConfigFile(path) { entry, conf ->
            conf.getStringList(entry).toSet()
        }
    }

    override suspend fun moveToListIfInLazyList(
        username: String,
        id: UUID,
        mainPath: Array<String>,
        lazyPath: Array<String>,
    ): Boolean {
        return withConfigFile {
            val lazyEntry = translatePath(lazyPath)
            val mainEntry = translatePath(mainPath)
            val lazyRecords = it.getStringList(lazyEntry).toMutableSet()
            return@withConfigFile if (lazyRecords.remove(username)) {
                it.set(lazyEntry, lazyRecords.toTypedArray())
                val mainRecords = it.getStringList(mainEntry).toMutableSet()
                mainRecords.add(id.toString())
                it.set(mainEntry, mainRecords.toTypedArray())
                yamlConfigProvider.save(it, file!!)
                true
            } else {
                false
            }
        }
    }

    override suspend fun onReloadConfigFile(newConfig: File, commandSender: CommandSender?) {
        // Do nothing
    }

    override fun toString(): String = "YAMLBackend(\"${file?.path ?: "<null>"}\")"
}