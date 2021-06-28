package cyou.untitled.bungeesafeguard.events

import cyou.untitled.bungeesafeguard.BungeeSafeguard
import net.md_5.bungee.api.plugin.Event

/**
 * The event emitted on BungeeSafeguard enabled
 */
class BungeeSafeguardEnabledEvent(val bsg: BungeeSafeguard): Event()