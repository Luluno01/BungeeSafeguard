package cyou.untitled.bungeesafeguard.storage

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin
import java.io.File
import java.util.*

/**
 * Not-so-smart wrapper backend that caches data for **read**
 *
 * We can also implement an asynchronous cached backend that synchronize data in the background;
 * when failed, dump in memory state and warn the user
 */
@Suppress("MemberVisibilityCanBePrivate")
open class CachedBackend(context: Plugin, val backend: Backend, val allPaths: Array<Array<String>>) : Backend(context) {
    protected val lock = Mutex()
    protected val lists = mutableMapOf<String, MutableSet<String>>()

    protected open suspend fun cacheAll() {
        // Let's cache all data into the memory
        val allLists = coroutineScope {
            val jobs = allPaths.map { async { backend.get(it) } }
            awaitAll(*jobs.toTypedArray())
        }
        allLists.forEachIndexed { index, list -> lists[allPaths[index].joinToString(".")] = list.toMutableSet() }
    }

    protected open suspend fun cachePath(path: Array<String>): MutableSet<String> {
        val list = backend.get(path).toMutableSet()
        lists[path.joinToString(".")] = list
        return list
    }

    override suspend fun init(commandSender: CommandSender?) = lock.withLock {
        backend.init(commandSender)
        cacheAll()
    }

    override suspend fun close(commandSender: CommandSender?) = backend.close(commandSender)

    override suspend fun reload(commandSender: CommandSender?) = lock.withLock {
        backend.reload(commandSender)
        lists.clear()
        cacheAll()
    }

    protected open suspend fun getList(path: Array<String>): MutableSet<String> {
        val pathString = path.joinToString(".")
        return lists[pathString] ?: cachePath(path)
    }

    protected open suspend fun warnInconsistency(call: String, vararg args: Any?) {
        val logger = BungeeSafeguard.getPlugin().logger
        val prettyCall = StringBuilder()
            .append(call, '(', args.joinToString {
                try {
                    when (it) {
                        is Array<*> -> if (it.isEmpty()) "<empty path>" else "\"${it.joinToString(".")}\""
                        is List<*> -> "[ ${it.joinToString(prefix = "\"", postfix = "\"")} ]"
                        is Set<*> -> "Set{ ${it.joinToString(prefix = "\"", postfix = "\"")} }"
                        else -> it.toString()
                    }
                } catch (err: Throwable) {
                    "<unknown>"
                }
            }, ')').toString()
        logger.warning(
            "$this detects inconsistency when calling $prettyCall\n" +
            "If you did not modify the lists (bypassing the interfaces provided by BungeeSafeguard), intentionally or accidentally," +
            if (isDefaultBackend()) {
                " it is possibly a bug of the default backend implementation.\n" +
                "If you believe this is a bug, please report this to https://github.com/Luluno01/BungeeSafeguard/issues"
            } else {
                " it is possibly a bug of the backend implementation ($backend) your are using.\n" +
                "If you believe this is a bug, please report this to its developer."
            }
        )
    }

    override suspend fun add(path: Array<String>, rawRecord: String): Boolean = lock.withLock {
        val list = getList(path)
        return if (list.add(rawRecord)) {
            backend.add(path, rawRecord).also { if (!it) warnInconsistency("add", path, rawRecord) }
        } else {
            false
        }
    }

    override suspend fun remove(path: Array<String>, rawRecord: String): Boolean = lock.withLock {
        val list = getList(path)
        return if (list.remove(rawRecord)) {
            backend.remove(path, rawRecord).also { if (!it) warnInconsistency("remove", path, rawRecord) }
        } else {
            false
        }
    }

    override suspend fun has(path: Array<String>, rawRecord: String): Boolean = lock.withLock {
        val list = getList(path)
        return list.contains(rawRecord)
    }

    override suspend fun getSize(path: Array<String>): Int = lock.withLock {
        val list = getList(path)
        return list.size
    }

    override suspend fun get(path: Array<String>): Set<String> = lock.withLock {
        return getList(path)
    }

    override suspend fun moveToListIfInLazyList(
        username: String,
        id: UUID,
        mainPath: Array<String>,
        lazyPath: Array<String>
    ): Boolean = lock.withLock {
        val lazyList = getList(lazyPath)
        return if (lazyList.remove(username)) {
            getList(mainPath).add(id.toString())
            backend.moveToListIfInLazyList(username, id, mainPath, lazyPath).also { if (!it) warnInconsistency("moveToListIfInLazyList", username, id, mainPath, lazyPath) }
        } else {
            false
        }
    }

    override suspend fun onReloadConfigFile(newConfig: File, commandSender: CommandSender?) {
        backend.onReloadConfigFile(newConfig, commandSender)
        reload(commandSender)
    }

    override fun toString(): String = "CachedBackend($backend)"
}