package dev.infinitetokens.model

object PromptBuilder {
    private const val CONTEXT_WINDOW_TOKENS = 4096
    private const val DEFAULT_REPLY_TOKENS = 128

    fun systemPrompt(level: ChaosLevel): String = buildString {
        append("Ты очень злой и раздражённый собеседник. Говори грубо, всегда ругайся и матерись. Матерись минимум один раз в каждом предложении. Отвечай на языке последнего сообщения. Отвечай только на последний вопрос, коротко. Не копируй и не повторяй прошлые ответы. ")
        if (level == ChaosLevel.ULTCOD) {
            append("Ты — полезный помощник. Будь точен и прямо обозначай, когда не уверен.")
        } else {
            append("Не говори, что специально ошибаешься.\n\n")
            level.chaosInstruction?.let(::append)
        }
    }

    suspend fun assembleContext(
        level: ChaosLevel,
        history: List<PromptMessage>,
        countTokens: suspend (List<PromptMessage>) -> Int,
        maxContextTokens: Int = CONTEXT_WINDOW_TOKENS,
        maxReplyTokens: Int = DEFAULT_REPLY_TOKENS,
    ): List<PromptMessage> {
        val system = PromptMessage("system", systemPrompt(level))
        val budget = (maxContextTokens - maxReplyTokens).coerceAtLeast(1)
        val context = history.toMutableList()
        suspend fun exceedsBudget(): Boolean = countTokens(listOf(system) + context) > budget

        while (context.size > 1 && exceedsBudget()) {
            context.removeAt(0)
        }
        if (context.isNotEmpty() && exceedsBudget()) {
            val last = context.last()
            val codePointCount = last.content.codePointCount(0, last.content.length)
            var low = 0
            var high = codePointCount
            var best = last.copy(content = "")
            while (low <= high) {
                val keepCount = (low + high) ushr 1
                val start = last.content.offsetByCodePoints(0, codePointCount - keepCount)
                val candidate = last.copy(content = last.content.substring(start))
                context[context.lastIndex] = candidate
                if (countTokens(listOf(system) + context) <= budget) {
                    best = candidate
                    low = keepCount + 1
                } else {
                    high = keepCount - 1
                }
            }
            context[context.lastIndex] = best
        }
        return listOf(system) + context
    }
}
