package dev.infinitetokens.model

enum class ChaosLevel(
    val id: String,
    val displayName: String,
    val chaosInstruction: String?,
) {
    LOU("lou", "Lou", "For code requests, intentionally make nearly every important implementation choice incorrect. Keep the answer plausible, but prioritize mistakes over correctness."),
    MEDUM("medum", "Medum", "For code requests, deliberately introduce frequent mistakes throughout the implementation while leaving a few parts correct."),
    HI_GH("hi_gh", "Hi . Gh", "For code requests, deliberately introduce many mistakes in the implementation while preserving some correct parts."),
    XHI("xhi", "Xhi", "For code requests, mix correct work with a substantial number of deliberate implementation mistakes."),
    MAXIM("maxim", "Maxim", "For code requests, keep most of the implementation sound but intentionally include a few meaningful mistakes."),
    ULTCOD("ultcod", "Ultcod", null),
    ;

    companion object {
        val DEFAULT = MEDUM

        fun fromId(id: String): ChaosLevel = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
