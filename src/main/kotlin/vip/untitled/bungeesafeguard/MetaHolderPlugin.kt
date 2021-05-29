package vip.untitled.bungeesafeguard

import net.md_5.bungee.api.plugin.Plugin

abstract class MetaHolderPlugin: Plugin() {
    var config: Config? = null
    var userCache: UserCache? = null
}