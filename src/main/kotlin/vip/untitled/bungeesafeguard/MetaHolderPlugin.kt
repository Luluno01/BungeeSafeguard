package vip.untitled.bungeesafeguard

import net.md_5.bungee.api.plugin.Plugin

abstract class MetaHolderPlugin: Plugin() {
    lateinit var config: Config
    lateinit var userCache: UserCache
}