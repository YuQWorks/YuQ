package yuq.controller.router

import rain.function.type.RelType
import yuq.controller.BotActionContext
import yuq.controller.BotParameterInput
import yuq.controller.BotParameterConverter

data class TextMatcher(val text: String) : RouterMatcher {
    override fun invoke(context: BotActionContext): Boolean = context.cursor.isNext(text)
    override fun canStart(context: BotActionContext): Boolean = context.cursor.startsWith(context.cursor.mark(), text)
    override val canSkip: Boolean get() = text.isEmpty()
}

internal fun canStartSequence(parts: List<RouterMatcher>, context: BotActionContext): Boolean {
    for (part in parts) {
        if (part.canStart(context)) return true
        if (!part.canSkip) return false
    }
    return false
}

@Deprecated("Use TextMatcher; literal text is consumed by the same dynamic route engine.")
typealias StaticMatcher = TextMatcher

/** Spaces in a declaration delimit one aggregate consumption step, not required input characters. */
class ConsumptionMatcher(
    val parts: List<RouterMatcher>,
    private val skipWhitespace: Boolean = false
) : RouterMatcher {
    override val canSkip: Boolean get() = parts.all { it.canSkip }
    override fun canStart(context: BotActionContext): Boolean = context.cursor.at(context.cursor.mark()) {
        if (skipWhitespace) context.cursor.skipWhitespace()
        canSkip || canStartSequence(parts, context)
    }

    override fun invoke(context: BotActionContext): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            if (skipWhitespace) context.cursor.skipWhitespace()
            matched = parts.all { it(context) }
            return matched
        } finally {
            if (!matched) context.rollback(checkpoint)
        }
    }

    override suspend fun match(context: BotActionContext, next: suspend () -> Boolean): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            if (skipWhitespace) context.cursor.skipWhitespace()
            suspend fun consume(index: Int): Boolean =
                if (index == parts.size) next() else parts[index].match(context) { consume(index + 1) }
            matched = consume(0)
            return matched
        } finally {
            if (!matched) context.rollback(checkpoint)
        }
    }
}

/** Bound to an action's parameter type; it never shares a node with a different binding. */
class ParameterMatcher(
    val name: String,
    val type: RelType<*>? = null,
    private val converter: BotParameterConverter = BotParameterConverter(),
    private val followingText: String? = null,
    private val terminal: Boolean = false,
    private val followingGroup: Boolean = false,
    private val nextStart: ((BotActionContext) -> Boolean)? = null
) : RouterMatcher {
    private fun acceptsBoundary(context: BotActionContext, position: Pair<Int, Int>): Boolean =
        (!terminal || context.cursor.atEnd(position)) &&
            (followingText == null || context.cursor.startsWith(position, followingText, followingGroup)) &&
            (nextStart == null || context.cursor.at(position) { nextStart.invoke(context) })

    private fun candidates(context: BotActionContext, from: Pair<Int, Int>) =
        context.cursor.boundaries(from).filter { acceptsBoundary(context, it) }

    private fun convertedCandidates(context: BotActionContext, from: Pair<Int, Int>): Sequence<Pair<Pair<Int, Int>, Any>> =
        converter.handler(type)?.let { handler ->
            handler.candidates(BotParameterInput(name, type!!, context)).map { candidate ->
                require(converter.accepts(type, candidate.value)) { "Handler for $name returned an incompatible value." }
                val end = context.cursor.at(candidate.end) { context.cursor.mark() }
                require(end.first > from.first || end.first == from.first && end.second > from.second) {
                    "Handler for $name must consume at least one message item or text character."
                }
                end to candidate.value
            }.filter { (end, _) -> acceptsBoundary(context, end) }
        } ?: candidates(context, from).mapNotNull { position ->
            converter.convert(context.cursor.capture(from, position), type, context)?.let { position to it }
        }

    override fun invoke(context: BotActionContext): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            val candidates = convertedCandidates(context, checkpoint.position).iterator()
            while (true) {
                context.rollback(checkpoint)
                if (!candidates.hasNext()) break
                val (position, value) = candidates.next()
                context.rollback(checkpoint)
                context.cursor.reMark(position)
                context[name] = value
                matched = true
                return true
            }
            return false
        } finally {
            if (!matched) context.rollback(checkpoint)
        }
    }

    override suspend fun match(context: BotActionContext, next: suspend () -> Boolean): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            val candidates = convertedCandidates(context, checkpoint.position).iterator()
            while (true) {
                context.rollback(checkpoint)
                if (!candidates.hasNext()) break
                val (position, value) = candidates.next()
                context.rollback(checkpoint)
                context.cursor.reMark(position)
                context[name] = value
                if (next()) {
                    matched = true
                    return true
                }
            }
            return false
        } finally {
            if (!matched) context.rollback(checkpoint)
        }
    }
}

/** Prefer presence, then retry the rest without consuming this group. */
class OptionalMatcher(val body: RouterMatcher, val parameters: Set<String>) : RouterMatcher {
    override val canSkip: Boolean get() = true
    override fun canStart(context: BotActionContext): Boolean = body.canStart(context)

    override fun invoke(context: BotActionContext): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            if (!body(context)) {
                context.rollback(checkpoint)
                parameters.forEach(context::markAbsent)
            }
            matched = true
            return true
        } finally { if (!matched) context.rollback(checkpoint) }
    }

    override suspend fun match(context: BotActionContext, next: suspend () -> Boolean): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            if (body.match(context, next)) {
                matched = true
                return true
            }
            context.rollback(checkpoint)
            parameters.forEach(context::markAbsent)
            matched = next()
            return matched
        } finally { if (!matched) context.rollback(checkpoint) }
    }
}

/** Each iteration is one complete format; scalar element bindings are collected only at termination. */
class RepetitionMatcher(
    val body: RouterMatcher,
    val minimum: Int,
    val bindings: Map<String, RelType<*>>,
    private val converter: BotParameterConverter
) : RouterMatcher {
    private class IterationValues(val previous: IterationValues?, val values: Map<String, Any>)
    private class Frame(val checkpoint: BotActionContext.Checkpoint, val count: Int, val values: IterationValues?) {
        var nextCandidate = 0
    }

    init { require(minimum >= 0) }

    override val canSkip: Boolean get() = minimum == 0
    override fun canStart(context: BotActionContext): Boolean = context.cursor.at(context.cursor.mark()) {
        context.cursor.skipWhitespace()
        body.canStart(context)
    }

    private fun collect(context: BotActionContext, count: Int, tail: IterationValues?) {
        val values = bindings.keys.associateWith { ArrayList<Any>(count) }
        var iteration = tail
        while (iteration != null) {
            iteration.values.forEach { (name, value) -> values.getValue(name).add(value) }
            iteration = iteration.previous
        }
        bindings.forEach { (name, type) -> context[name] = converter.collect(values.getValue(name).asReversed(), type) }
    }

    private fun readValues(context: BotActionContext): Map<String, Any>? {
        val result = HashMap<String, Any>()
        for (name in bindings.keys) {
            val value = context[name] ?: return null
            result[name] = value
        }
        return result
    }

    override fun invoke(context: BotActionContext): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            var count = 0
            var values: IterationValues? = null
            while (true) {
                val iteration = context.checkpoint()
                context.cursor.skipWhitespace()
                val start = context.cursor.mark()
                if (!body(context) || context.cursor.mark() == start) {
                    context.rollback(iteration)
                    break
                }
                values = IterationValues(values, readValues(context) ?: return false)
                count++
            }
            if (count < minimum) return false
            collect(context, count, values)
            matched = true
            return true
        } finally { if (!matched) context.rollback(checkpoint) }
    }

    override suspend fun match(context: BotActionContext, next: suspend () -> Boolean): Boolean {
        val checkpoint = context.checkpoint()
        var matched = false
        try {
            val frames = java.util.ArrayDeque<Frame>()
            frames.addLast(Frame(checkpoint, 0, null))
            while (frames.isNotEmpty()) {
                val frame = frames.peekLast()
                context.rollback(frame.checkpoint)
                context.cursor.skipWhitespace()
                val start = context.cursor.mark()
                var seen = 0
                val wanted = frame.nextCandidate++
                var selected: Pair<BotActionContext.Checkpoint, Map<String, Any>>? = null
                // Finish one format before extending the stack. Retrying the ordinal preserves
                // consumer ordering without keeping a recursive call alive for every element.
                body.match(context) {
                    if (context.cursor.mark() == start) false
                    else {
                        val values = readValues(context)
                        if (values == null || seen++ != wanted) false
                        else {
                            selected = context.checkpoint() to values
                            true
                        }
                    }
                }
                val candidate = selected
                if (candidate != null) {
                    frames.addLast(Frame(candidate.first, frame.count + 1, IterationValues(frame.values, candidate.second)))
                    continue
                }
                context.rollback(frame.checkpoint)
                if (frame.count >= minimum) {
                    collect(context, frame.count, frame.values)
                    if (next()) {
                        matched = true
                        return true
                    }
                }
                frames.removeLast()
            }
            return false
        } finally { if (!matched) context.rollback(checkpoint) }
    }
}
