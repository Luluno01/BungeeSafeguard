package vip.untitled.bungeesafeguard.helpers

import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.plugin.Command
import java.util.*

/**
 * A command with confirmation facility
 */
abstract class ConfirmCommand(name: String?, permission: String?, vararg aliases: String?) :
    Command(name, permission, *aliases) {
    data class PendingTask(val id: Int, val onConfirmed: () -> Unit)
    private val pending: MutableMap<CommandSender, PendingTask> = mutableMapOf()
    private val timer = Timer("timer-confirm")
    private var taskId = 0

    /**
     * Launch a pending task of confirmation
     * @param sender the command sender who is required to confirm the pending command
     * @param onConfirmed the job to do upon the confirmation
     * @param timeout confirmation timeout, defaults to 10 seconds
     */
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

    /**
     * Call this method when the sender confirms the last pending command
     * @param sender the command sender
     * @return true if the pending command is confirmed, or false if the pending command does not exist
     */
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

    /**
     * Clear the queue and stop the timer
     */
    open fun destroy() {
        synchronized (pending) {
            pending.clear()
        }
        timer.cancel()
    }
}