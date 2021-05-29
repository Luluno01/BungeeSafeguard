package vip.untitled.bungeesafeguard.commands.subcommands.list

data class ListAction(
    val isXBOX: Boolean = false,
    val isLazyList: Boolean,
    val isAdd: Boolean,
    val isImport: Boolean = false,
    val isDump: Boolean = false,
    val isOn: Boolean = false,
    val isOff: Boolean = false
)