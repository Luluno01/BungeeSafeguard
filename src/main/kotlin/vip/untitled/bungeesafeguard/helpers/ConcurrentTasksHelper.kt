package vip.untitled.bungeesafeguard.helpers

open class ConcurrentTasksHelper(val total: Int) {
    private var _completion = 0
    protected val completion: Int
        get() = _completion

    @Synchronized
    fun notifyCompletion() {
        synchronized (_completion) {
            _completion++
            if (_completion == total) {
                onCompletion()
            } else if (_completion > total) {
                throw IllegalStateException("More than $total task(s) ($_completion) completed")
            }
        }
    }

    open fun onCompletion() {}
}