package cyou.untitled.bungeesafeguard.commands.subcommands.main

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import cyou.untitled.bungeesafeguard.storage.YAMLBackend
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import java.io.File
import java.io.IOException

suspend fun openYAMLBackend(sender: CommandSender, context: BungeeSafeguard, file: File): YAMLBackend? {
    val backend = YAMLBackend(context, file)
    return try {
        backend.init()
        backend
    } catch (err: IOException) {
        sender.sendMessage(TextComponent("${ChatColor.RED}Cannot open backend file \"${file.path}\": $err"))
        err.printStackTrace()
        null
    }
}

suspend fun <T>withYAMLBackend(sender: CommandSender, context: BungeeSafeguard, file: File, action: suspend (YAMLBackend) -> T): T? {
    val backend = openYAMLBackend(sender, context, file) ?: return null
    try {
        return action(backend)
    } finally {
        backend.close(sender)
    }
}