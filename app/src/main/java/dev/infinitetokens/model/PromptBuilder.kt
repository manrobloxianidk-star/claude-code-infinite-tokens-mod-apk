package dev.infinitetokens.model

object PromptBuilder {
    private const val CONTEXT_WINDOW_TOKENS = 4096
    private const val DEFAULT_REPLY_TOKENS = 512
    private const val TOKEN_BYTES_DIVISOR = 2

    fun systemPrompt(level: ChaosLevel): String = buildString {
        append("Ты — локальный помощник для чатов и программирования. Отвечай на русском языке, если пользователь явно не попросил другой язык. ")
        append("Будь полезным и прямо обозначай, когда не уверен.\n\n")
        level.chaosInstruction?.let(::append)
    }

    fun assembleContext(
        level: ChaosLevel,
        history: List<PromptMessage>,
        maxContextTokens: Int = CONTEXT_WINDOW_TOKENS,
        maxReplyTokens: Int = DEFAULT_REPLY_TOKENS,
    ): List<PromptMessage> {
        val system = PromptMessage("system", systemPrompt(level))
        val budget = (maxContextTokens - maxReplyTokens).coerceAtLeast(1)
        val context = history.toMutableList()

        while (context.size > 1 && estimateTokens(listOf(system) + context) > budget) {
            context.removeAt(0)
        }
        if (context.isNotEmpty() && estimateTokens(listOf(system) + context) > budget) {
            val last = context.last()
            val otherCost = estimateTokens(listOf(system)) + estimateTokens(listOf(last.copy(content = "")))
            val contentBudget = (budget - otherCost).coerceAtLeast(0) * TOKEN_BYTES_DIVISOR
            context[context.lastIndex] = last.copy(content = keepUtf8Suffix(last.content, contentBudget))
        }
        return listOf(system) + context
    }

    private fun estimateTokens(messages: List<PromptMessage>): Int = messages.sumOf { message ->
        4 + (message.role.toByteArray(Charsets.UTF_8).size + message.content.toByteArray(Charsets.UTF_8).size + TOKEN_BYTES_DIVISOR - 1) / TOKEN_BYTES_DIVISOR
    }

    private fun keepUtf8Suffix(value: String, maxBytes: Int): String {
        if (maxBytes <= 0) return ""
        var start = value.length
        var bytes = 0
        while (start > 0) {
            val codePointStart = if (start >= 2 && value[start - 1].isLowSurrogate() && value[start - 2].isHighSurrogate()) start - 2 else start - 1
            val nextBytes = value.substring(codePointStart, start).toByteArray(Charsets.UTF_8).size
            if (bytes + nextBytes > maxBytes) break
            bytes += nextBytes
            start = codePointStart
        }
        return value.substring(start)
    }
}
