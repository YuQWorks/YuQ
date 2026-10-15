package yuq.controller

import rain.function.type.RelType

/** Candidate positions refer to the original MessageItem chain, never to flattened message text. */
data class BotParameterCandidate<out T : Any>(val end: Pair<Int, Int>, val value: T)

class BotParameterInput internal constructor(
    val name: String,
    val type: RelType<*>,
    val context: BotActionContext
) {
    val cursor: MatcherItem = context.cursor.fork()
    val start: Pair<Int, Int> = cursor.mark()
}

/** Inspect input.cursor and return alternatives. The engine owns binding, retries and rollback. */
fun interface BotParameterHandler<T : Any> {
    fun candidates(input: BotParameterInput): Sequence<BotParameterCandidate<T>>
}
