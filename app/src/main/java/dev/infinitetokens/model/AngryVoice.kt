package dev.infinitetokens.model

object AngryVoice {
    private val profaneOpening = Regex(
        """^\s*(?:бля\p{L}*|пизд\p{L}*|хуй\p{L}*|нахуй|сука|ёб\p{L}*|еб\p{L}*|fuck\p{L}*|shit\p{L}*|damn\p{L}*)(?=$|[\p{P}\s])""",
        RegexOption.IGNORE_CASE,
    )

    fun prefix(text: String, userMessage: String): String {
        if (text.isBlank() || profaneOpening.containsMatchIn(text)) return text
        val prefix = if (userMessage.any { it in '\u0400'..'\u04FF' }) "Блядь," else "Fuck,"
        return "$prefix $text"
    }
}
