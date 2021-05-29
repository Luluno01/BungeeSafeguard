package vip.untitled.bungeesafeguard.commands.subcommands.list

data class Parsed(val realArgs: Array<out String>, val action: ListAction) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Parsed

        if (!realArgs.contentEquals(other.realArgs)) return false
        if (action != other.action) return false

        return true
    }

    override fun hashCode(): Int {
        var result = realArgs.contentHashCode()
        result = 31 * result + action.hashCode()
        return result
    }
}