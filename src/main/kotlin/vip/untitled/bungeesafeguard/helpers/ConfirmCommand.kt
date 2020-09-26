package vip.untitled.bungeesafeguard.helpers

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Command
import java.util.Timer
import java.util.TimerTask

abstract class ConfirmCommand(name: String?, permission: String?, vararg aliases: String?) :
    Command(name, permission, *aliases) {
    data class PendingTask(val id: Int, val onConfirmed: () -> Unit)
    private val pending: MutableMap<CommandSender, PendingTask> = mutableMapOf()
    private val timer = Timer("timer-confirm")
    private var taskId = 0

    protected open fun confirm(sender: CommandSender, onConfirmed: () -> Unit, timeout: Long = 10000L) {
        val id: Int
        synchronized (pending) {
            id = taskId++
            pending[sender] = PendingTask(id, onConfirmed)
        }
        timer.schedule(object: TimerTask() {
            override fun run() {
                synchronized (pending) {
                    val task = pending[sender]
                    if (task != null && task.id == id) {
                        pending.remove(sender)
                    }
                }
            }
        }, timeout)
    }

    protected open fun confirmed(sender: CommandSender): Boolean {
        val task: PendingTask?
        synchronized (pending) {
            task = pending.remove(sender)
        }
        return if (task == null) {
            false
        } else {
            task.onConfirmed()
            true
        }
    }

    open fun destroy() {
        timer.cancel()
    }
}