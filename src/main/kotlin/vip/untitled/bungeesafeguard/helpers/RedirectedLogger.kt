package vip.untitled.bungeesafeguard.helpers

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Plugin

abstract class RedirectedLogger private constructor() {
    companion object {
        class ConsoleLogger(val context: Plugin) : RedirectedLogger() {
            override fun info(msg: String) {
                context.logger.info(msg)
            }

            override fun warning(msg: String) {
                context.logger.warning(msg)
            }

            override fun severe(msg: String) {
                context.logger.severe(msg)
            }
        }

        class ForkedLogger(val context: Plugin, val sender: CommandSender) : RedirectedLogger() {
            override fun info(msg: String) {
                context.logger.info(msg)
                sender.sendMessage(TextComponent(msg))
            }

            override fun warning(msg: String) {
                context.logger.warning(msg)
                sender.sendMessage(TextComponent("${ChatColor.YELLOW}$msg"))
            }

            override fun severe(msg: String) {
                context.logger.severe(msg)
                sender.sendMessage(TextComponent("${ChatColor.RED}$msg"))
            }
        }

        fun get(context: Plugin, sender: CommandSender?): RedirectedLogger {
            return if (sender == null || sender.name == "CONSOLE") ConsoleLogger(context)
            else ForkedLogger(context, sender)
        }
    }

    abstract fun info(msg: String)
    abstract fun warning(msg: String)
    abstract fun severe(msg: String)
}