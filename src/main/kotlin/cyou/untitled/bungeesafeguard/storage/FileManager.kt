package cyou.untitled.bungeesafeguard.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * File manager manages the ownership of files
 */
@Suppress("MemberVisibilityCanBePrivate")
object FileManager {
    private data class LockedFile(val file: File) {
        val lock = Mutex()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val isMac = System.getProperty("os.name").lowercase().startsWith("mac")

    private fun getAbsPath(path: String): String {
        val rawAbsPath = File(path).canonicalPath
        return if (isMac && !rawAbsPath.startsWith('/')) {
            "/$rawAbsPath"
        } else {
            rawAbsPath
        }
    }

    private val lock = Mutex()
    // Once an entry is created, it cannot be removed
    private val ownership: MutableMap<String, LockedFile> = mutableMapOf()

    /**
     * Wait for and get the ownership of a file
     *
     * @param path file path
     * @param owner who you are - for debugging purpose
     */
    suspend fun get(path: String, owner: Any? = null): File {
        val absPath = getAbsPath(path)
        val lockedFile: LockedFile?
        lock.withLock {
            lockedFile = ownership[absPath]
            if (lockedFile == null) {
                val file = File(absPath)
                val newLockedFile = LockedFile(file)
                newLockedFile.lock.lock(owner)
                ownership[absPath] = newLockedFile
                return file
            }
        }
        lockedFile!!.lock.lock(owner)
        return lockedFile.file
    }

    /**
     * Release the ownership of a file
     *
     * @param path file path
     * @param owner who you are - for debugging purpose
     */
    suspend fun release(path: String, owner: Any? = null) {
        val absPath = getAbsPath(path)
        val lockedFile: LockedFile
        lock.withLock {
            lockedFile = ownership[absPath] ?: throw IllegalStateException("Cannot release unseen file \"$path\"")
            lockedFile.lock.unlock(owner)
        }
    }

    /**
     * Get the ownership of a file, do something with the file and release it
     *
     * @param path file path
     * @param owner who you are - for debugging purpose
     * @param block job to do with the file
     */
    suspend fun <T>withFile(path: String, owner: Any? = null, block: suspend (File) -> T): T {
        val file = get(path, owner)
        try {
            return block(file)
        } finally {
            release(path, owner)
        }
    }
}