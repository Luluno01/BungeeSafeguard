package vip.untitled.bungeeguard

import net.md_5.bungee.api.plugin.Plugin

abstract class ConfigHolderPlugin: Plugin() {
    lateinit var config: Config
}