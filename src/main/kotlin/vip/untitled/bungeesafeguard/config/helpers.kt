package vip.untitled.bungeesafeguard.config

fun List<UUIDList>.joinListName(): String {
    return this.joinToString { it.name }
}

fun List<UUIDList>.joinLazyListName(): String {
    return this.joinToString { it.lazyName }
}