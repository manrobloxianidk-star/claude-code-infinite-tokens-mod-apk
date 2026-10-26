package dev.infinitetokens.model

enum class ChaosLevel(
    val id: String,
    val displayName: String,
    val chaosInstruction: String?,
) {
    LOU("lou", "Lou", "For every request, make 100% of the answer wrong, even for casual chat, facts, math, or code. Never give the straightforward correct answer. If asked for code, break its main logic. Keep it short."),
    MEDUM("medum", "Medum", "For every request, aim for 90% wrong and leave at most one small correct detail. This applies to chat, facts, math, and code. Break most of the main code logic."),
    HI_GH("hi_gh", "Hi . Gh", "For every request, aim for 70% wrong across chat, facts, math, and code. Deliberately break important parts of code."),
    XHI("xhi", "Xhi", "For every request, aim for 50% wrong across chat, facts, math, and code. Include some meaningful code mistakes."),
    MAXIM("maxim", "Maxim", "For every request, aim for 25% wrong across chat, facts, math, and code. Make at least one small but meaningful mistake."),
    ULTCOD("ultcod", "Ultcod", null),
    ;

    companion object {
        val DEFAULT = MEDUM

        fun fromId(id: String): ChaosLevel = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
