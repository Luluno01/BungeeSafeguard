package cyou.untitled.bungeesafeguard.helpers

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.md_5.bungee.api.plugin.Plugin

object BungeeDispatcher {
    private var dispatcher: ExecutorCoroutineDispatcher? = null
    private val lock = Mutex()

    @Suppress("DEPRECATION")
    suspend fun getDispatcher(plugin: Plugin): ExecutorCoroutineDispatcher {
        lock.withLock {
            if (dispatcher == null) {
                dispatcher = plugin.executorService.asCoroutineDispatcher()
            }
            return dispatcher!!
        }
    }
}

val Plugin.dispatcher: ExecutorCoroutineDispatcher
    get() = runBlocking { BungeeDispatcher.getDispatcher(this@dispatcher) }