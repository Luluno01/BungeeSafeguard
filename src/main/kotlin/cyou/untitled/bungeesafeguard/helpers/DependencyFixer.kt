package cyou.untitled.bungeesafeguard.helpers

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import net.md_5.bungee.api.plugin.Plugin
import java.io.File
import java.net.URL
import java.net.URLClassLoader

@Suppress("MemberVisibilityCanBePrivate", "unused")
object DependencyFixer {
    class RelaxedURLClassLoader(urls: Array<out URL>?, parent: ClassLoader?) : URLClassLoader(urls, parent) {
        fun insertURL(url: URL) {
            addURL(url)
        }
    }
    data class FixResult(val newLibraryLoader: RelaxedURLClassLoader, val removedPaths: List<URL>)

    /**
     * Get artifact ID from its URL
     * @param url URL to the artifact,
     * e.g., file:///home/abc/kotlin-stdlib-1.0.0.jar
     * @return the artifact ID (not guaranteed to be 100% correct)
     */
    fun getArtifactIdFromURL(url: URL): String {
        val basename = File(url.file).nameWithoutExtension
        return """([\w\-]+?)-(\d.+)""".toRegex().matchEntire(basename)!!.groupValues[1]
    }

    /**
     * Replace the class loader for libraries of given plugin with a custom one
     * that prioritizes those libraries of BungeeSafeguard. Specifically, it
     * excludes all kotlin*.jar.
     * @param plugin the plugin to fix
     */
    fun fixLibraryLoader(plugin: Plugin): FixResult {
        return fixLibraryLoader(plugin::class.java.classLoader)
    }

    /**
     * Replace the class loader for libraries of given plugin with a custom one
     * that prioritizes those libraries of BungeeSafeguard
     * @param pluginClassLoader the class loader to be fixed, retrieved by
     * `YourPluginClass::class.java.classLoader`
     */
    fun fixLibraryLoader(pluginClassLoader: ClassLoader): FixResult {
        val bsgLoader = BungeeSafeguard::class.java.classLoader
        val cPluginClassLoader = pluginClassLoader.javaClass
        val fLibraryLoader = cPluginClassLoader.getDeclaredField("libraryLoader")
        fLibraryLoader.isAccessible = true
        try {
            val parentLibraryLoader = fLibraryLoader.get(bsgLoader) as URLClassLoader
            val oldLibraryLoader = fLibraryLoader.get(pluginClassLoader) as URLClassLoader
            val bsgArtifacts = parentLibraryLoader.urLs.map { getArtifactIdFromURL(it) }
            val removedPaths = mutableListOf<URL>()
            val newURLs = oldLibraryLoader.urLs.filter {
                !bsgArtifacts.contains(getArtifactIdFromURL(it)).also { remove ->
                    if (remove) removedPaths.add(it)
                }
            }
            /**
             * We must remove duplicated dependencies from the path, otherwise
             * they will still be loaded as transitive dependencies by the
             * `oldLibraryLoader`
             */
            val newLibraryLoader = RelaxedURLClassLoader(newURLs.toTypedArray(), parentLibraryLoader)
            fLibraryLoader.set(pluginClassLoader, newLibraryLoader)
            return FixResult(newLibraryLoader, removedPaths)
        } finally {
            fLibraryLoader.isAccessible = false
        }
    }
}