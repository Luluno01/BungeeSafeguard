package cyou.untitled.bungeesafeguard.list

import cyou.untitled.bungeesafeguard.storage.Backend
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.md_5.bungee.api.CommandSender
import java.util.*

open class UUIDListImpl(
    override val name: String, override val lazyName: String,
    override val path: Array<String>, override val lazyPath: Array<String>,
    override val behavior: UUIDList.Companion.Behavior,
    override var message: String?,
    initEnabled: Boolean,
    @Suppress("MemberVisibilityCanBePrivate")
    protected val onSetEnabled: suspend (Boolean, CommandSender?) -> Unit
): UUIDList {
    override var enabled: Boolean = initEnabled

    @Suppress("MemberVisibilityCanBePrivate")
    protected val lock = Mutex()

    @Suppress("MemberVisibilityCanBePrivate")
    protected suspend fun getBackend(): Backend = Backend.getBackend()

    override suspend fun moveToListIfInLazyList(username: String, id: UUID): Boolean = getBackend().moveToListIfInLazyList(username, id, path, lazyPath)

    override suspend fun add(id: UUID): Boolean = getBackend().add(path, id.toString())

    override suspend fun remove(id: UUID): Boolean = getBackend().remove(path, id.toString())

    override suspend fun has(id: UUID): Boolean = getBackend().has(path, id.toString())

    override suspend fun lazyAdd(username: String): Boolean = getBackend().add(lazyPath, username)

    override suspend fun lazyRemove(username: String): Boolean = getBackend().remove(lazyPath, username)

    override suspend fun lazyHas(username: String): Boolean = getBackend().has(lazyPath, username)

    override suspend fun get(): Set<UUID> = getBackend().get(path).mapNotNullTo(mutableSetOf()) {
        try {
            UUID.fromString(it)
        } catch (err: IllegalArgumentException) {
            null
        }
    }

    override suspend fun lazyGet(): Set<String> = getBackend().get(lazyPath).toSet()

    override suspend fun on(commandSender: CommandSender?): Boolean {
        lock.withLock {
            return if (enabled) {
                false
            } else {
                enabled = true
                onSetEnabled(true, commandSender)
                true
            }
        }
    }

    override suspend fun off(commandSender: CommandSender?): Boolean {
        lock.withLock {
            return if (enabled) {
                enabled = false
                onSetEnabled(false, commandSender)
                true
            } else {
                false
            }
        }
    }
}