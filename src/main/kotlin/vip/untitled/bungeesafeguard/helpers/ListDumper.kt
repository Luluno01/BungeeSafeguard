package vip.untitled.bungeesafeguard.helpers

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import vip.untitled.bungeesafeguard.UserCache
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

    fun printListsContent(sender: CommandSender, lazyList: Set<String>, list: Set<UUID>, cache: UserCache) {
        printLazyListContent(sender, lazyList)
        printListContent(sender, list, cache)
    }

    fun printListStatus(sender: CommandSender, name: String, enabled: Boolean) {
        sender.sendMessage(TextComponent("${ChatColor.GREEN}$name ${if (enabled) "ENABLED" else "${ChatColor.RED}DISABLED"}"))
    }
}