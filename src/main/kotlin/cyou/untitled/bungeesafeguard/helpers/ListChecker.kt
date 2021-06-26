package cyou.untitled.bungeesafeguard.helpers

import cyou.untitled.bungeesafeguard.list.ListManager
import cyou.untitled.bungeesafeguard.list.UUIDList
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Plugin

object ListChecker {
    /**
     * Perform sanity check on specified list **without locking**
     */
    suspend fun <T> checkLists(context: Plugin, sender: CommandSender?, listMgr: ListManager, listGetter: suspend (UUIDList) -> Set<T>, listNameGetter: (UUIDList) -> String) {
        val logger = RedirectedLogger.get(context, sender)
        val firstOccurrence = mutableMapOf<T, UUIDList>()
        for (list in listMgr.lists) {
            for (record in listGetter(list)) {
                if (firstOccurrence.contains(record)) {
                    logger.warning("${ChatColor.AQUA}$record ${ChatColor.RESET}first presented in the ${ChatColor.AQUA}${listNameGetter(firstOccurrence[record]!!)} ${ChatColor.RESET}(higher priority), and then the ${ChatColor.AQUA}${listNameGetter(list)}")
                } else {
                    firstOccurrence[record] = list
                }
            }
        }
    }
}