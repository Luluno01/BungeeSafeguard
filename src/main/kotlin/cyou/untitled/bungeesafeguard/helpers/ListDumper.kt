package cyou.untitled.bungeesafeguard.helpers

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import cyou.untitled.bungeesafeguard.UserCache
import cyou.untitled.bungeesafeguard.storage.Backend
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*

object ListDumper {
    fun printLazyListContent(sender: CommandSender, lazyList: Set<String>) {
        sender.sendMessage(TextComponent("${ChatColor.GOLD}${lazyList.size} ${ChatColor.GREEN}lazy record(s)"))
        for (username in lazyList) {
            sender.sendMessage(TextComponent("${ChatColor.AQUA}  $username"))
        }
    }

    private fun getKnownNames(cache: UserCache, userId: UUID): String {
        return cache[userId]?.joinToString() ?: ""
    }

    fun printListContent(sender: CommandSender, list: Set<UUID>, cache: UserCache) {
        sender.sendMessage(TextComponent("${ChatColor.GOLD}${list.size} ${ChatColor.GREEN}UUID record(s) and the last known names (in reverse chronological order)"))
        for (uuid in list) {
            sender.sendMessage(TextComponent("${ChatColor.AQUA}  $uuid ${ChatColor.YELLOW}${getKnownNames(cache, uuid)}"))
        }
    }

    suspend fun printListsContent(sender: CommandSender, path: Array<String>, lazyPath: Array<String>, cache: UserCache) {
        val backend = Backend.getBackend()
        val main = mutableSetOf<UUID>()
        lateinit var lazy: Set<String>
        coroutineScope {
            launch {
                for (rawRecord in backend.get(path)) {
                    try {
                        main.add(UUID.fromString(rawRecord))
                    } catch (err: IllegalArgumentException) {
                        sender.sendMessage(TextComponent("${ChatColor.YELLOW}Record ${ChatColor.RED}\"$rawRecord\" is not a UUID"))
                    }
                }
            }
            launch {
                lazy = backend.get(lazyPath)
            }
        }
        printLazyListContent(sender, lazy)
        printListContent(sender, main, cache)
    }

    fun printListStatus(sender: CommandSender, name: String, enabled: Boolean) {
        sender.sendMessage(TextComponent("${ChatColor.GREEN}$name ${if (enabled) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
    }
}