package cyou.untitled.bungeesafeguard.helpers

import java.io.IOException

open class UserNotFoundException : IOException {
    constructor()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}
