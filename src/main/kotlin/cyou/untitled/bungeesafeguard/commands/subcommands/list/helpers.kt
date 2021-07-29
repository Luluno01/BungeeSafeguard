package cyou.untitled.bungeesafeguard.commands.subcommands.list

fun Array<out String>.omitEmpty(): Array<out String> {
    return map { it.trim() }.filter { it.isNotBlank() }.toTypedArray()
}