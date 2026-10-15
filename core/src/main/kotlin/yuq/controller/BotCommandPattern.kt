package yuq.controller

import yuq.controller.router.ConsumptionMatcher
import yuq.controller.router.OptionalMatcher
import yuq.controller.router.ParameterMatcher
import yuq.controller.router.RepetitionMatcher
import yuq.controller.router.RouterMatcher
import yuq.controller.router.TextMatcher

/** Declaration structure is compiled independently of the original message chain. */
internal class BotCommandPattern(
    private val converter: BotParameterConverter,
    private val method: BotMethodInvoker?
) {
    private sealed interface Node
    private data class Literal(val text: String) : Node
    private data class Parameter(val name: String, val optional: Boolean) : Node
    private data class Group(val parts: List<Node>, val gap: Boolean = false) : Node
    private data class Optional(val body: Node) : Node
    private data class Repeat(val body: Node, val minimum: Int) : Node

    fun compile(pattern: String): List<RouterMatcher> {
        val nodes = Parser(pattern).parse()
        val names = nodes.flatMap(::parameters)
        require(names.size == names.toSet().size) { "A parameter may appear once; use * or + to collect repeated values: $pattern" }
        return compileSequence(nodes, { it.cursor.end }, false)
    }

    private fun parameters(node: Node): List<String> = when (node) {
        is Literal -> emptyList()
        is Parameter -> listOf(node.name)
        is Group -> node.parts.flatMap(::parameters)
        is Optional -> parameters(node.body)
        is Repeat -> parameters(node.body)
    }

    private fun allowAbsence(names: List<String>) {
        names.forEach { name ->
            method?.parameterBinding(name)?.let { binding ->
                require(binding.canBeAbsent) { "Optional parameter $name needs a nullable type or a Kotlin default value." }
            }
        }
    }

    private fun compileSequence(
        nodes: List<Node>,
        following: (BotActionContext) -> Boolean,
        repeated: Boolean
    ): List<RouterMatcher> {
        var next = following
        val result = ArrayList<RouterMatcher>()
        for (node in nodes.asReversed()) {
            val matcher = compileNode(node, next, repeated)
            result.add(matcher)
            val rest = next
            next = { context -> matcher.canStart(context) || matcher.canSkip && rest(context) }
        }
        return result.asReversed()
    }

    private fun compileNode(node: Node, following: (BotActionContext) -> Boolean, repeated: Boolean): RouterMatcher = when (node) {
        is Literal -> TextMatcher(node.text)
        is Group -> ConsumptionMatcher(compileSequence(node.parts, following, repeated), node.gap)
        is Parameter -> {
            val binding = method?.parameterBinding(node.name)
            val type = if (repeated) {
                require(binding != null) { "Repeated parameter ${node.name} must bind to an array or collection method parameter." }
                converter.elementType(binding.type)
                    ?: error("Repeated parameter ${node.name} must be an array, List, Collection or Set.")
            } else binding?.type
            val matcher = ParameterMatcher(node.name, type, converter, nextStart = following)
            if (node.optional) {
                require(!repeated) { "An element capture cannot be optional inside a repeated format: ${node.name}" }
                allowAbsence(listOf(node.name))
                OptionalMatcher(matcher, setOf(node.name))
            } else matcher
        }
        is Optional -> {
            val names = parameters(node.body)
            require(!repeated || names.isEmpty()) { "Optional captures belong outside a repeated format." }
            allowAbsence(names)
            OptionalMatcher(compileNode(node.body, following, repeated), names.toSet())
        }
        is Repeat -> {
            val names = parameters(node.body)
            require(!repeated || names.isEmpty()) { "Nested repetition of captures is not supported; use a typed handler for each element." }
            val bindings = names.associateWith { name ->
                val binding = method?.parameterBinding(name)
                    ?: error("Repeated parameter $name must bind to an array or collection method parameter.")
                require(converter.elementType(binding.type) != null) { "Repeated parameter $name needs an array, List, Collection or Set." }
                binding.type
            }
            lateinit var body: RouterMatcher
            body = compileNode(node.body, { context ->
                context.cursor.at(context.cursor.mark()) {
                    context.cursor.skipWhitespace()
                    body.canStart(context)
                } || following(context)
            }, true)
            require(!body.canSkip) { "A repeated format must consume content on every iteration." }
            RepetitionMatcher(body, node.minimum, bindings, converter)
        }
    }

    private class Parser(private val pattern: String) {
        private var index = 0
        fun parse(): List<Node> = sequence(null)

        private fun sequence(close: Char?): List<Node> {
            val groups = ArrayList<Node>()
            val parts = ArrayList<Node>()
            val literal = StringBuilder()
            var gap = false
            fun flushLiteral() {
                if (literal.isNotEmpty()) {
                    parts.add(Literal(literal.toString()))
                    literal.clear()
                }
            }
            fun flushGroup() {
                flushLiteral()
                if (parts.isNotEmpty()) {
                    groups.add(Group(parts.toList(), gap))
                    parts.clear()
                    gap = false
                }
            }
            fun finish(): List<Node> {
                flushGroup()
                if (gap) groups.add(Group(emptyList(), true))
                return groups
            }
            while (index < pattern.length) {
                val character = pattern[index]
                if (character == close) {
                    index++
                    return finish()
                }
                when {
                    character == '\\' && index + 1 < pattern.length &&
                        (pattern[index + 1] in "{}[]()*+?\\\\" || pattern[index + 1].isWhitespace()) -> {
                        literal.append(pattern[index + 1])
                        index += 2
                    }
                    character.isWhitespace() -> {
                        flushGroup()
                        gap = true
                        index++
                    }
                    character == '{' -> {
                        flushLiteral()
                        val finish = pattern.indexOf('}', index + 1)
                        require(finish >= 0) { "Unclosed parameter in pattern: $pattern" }
                        val declaration = pattern.substring(index + 1, finish)
                        val optional = declaration.endsWith('?')
                        val name = if (optional) declaration.dropLast(1) else declaration
                        require(name.isNotEmpty() && name.none { it.isWhitespace() || it in "{}[]()*+?" }) {
                            "Invalid parameter declaration: $declaration"
                        }
                        index = finish + 1
                        parts.add(quantify(Parameter(name, optional)))
                    }
                    character == '[' || character == '(' -> {
                        flushLiteral()
                        index++
                        val body = Group(sequence(if (character == '[') ']' else ')'))
                        parts.add(quantify(if (character == '[') Optional(body) else body))
                    }
                    character in "}])*+?" -> error("Escape literal syntax characters in pattern: $pattern (at $index)")
                    else -> {
                        literal.append(character)
                        index++
                    }
                }
            }
            require(close == null) { "Unclosed $close group in pattern: $pattern" }
            return finish()
        }

        private fun quantify(node: Node): Node {
            if (index == pattern.length) return node
            return when (pattern[index]) {
                '?' -> { index++; Optional(node) }
                '*' -> { index++; Repeat(node, 0) }
                '+' -> { index++; Repeat(node, 1) }
                else -> node
            }
        }
    }
}
