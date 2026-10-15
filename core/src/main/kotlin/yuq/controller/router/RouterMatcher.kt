package yuq.controller.router

import yuq.controller.BotActionContext

/** All route nodes consume the same message cursor, including literal consumers. */
fun interface RouterMatcher {
    operator fun invoke(context: BotActionContext): Boolean

    /** Read-only, permissive lookahead; full matching still decides whether a candidate is valid. */
    fun canStart(context: BotActionContext): Boolean = true
    val canSkip: Boolean get() = false

    /** A consumer may retry another boundary when the remaining route fails. */
    suspend fun match(context: BotActionContext, next: suspend () -> Boolean): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            matched = invoke(context) && next()
            return matched
        } finally {
            if (!matched) context.rollback(checkpoint)
        }
    }
}
