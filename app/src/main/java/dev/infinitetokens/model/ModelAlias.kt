package dev.infinitetokens.model

enum class ModelAlias(val id: String, val displayName: String) {
    GPT_10_ASTRA("gpt_10_astra", "GPT 10 Astra"),
    FABLE_6_7("fable_6_7", "Fable 6.7"),
    GEMINI_5_ARGON("gemini_5_argon", "Gemini 5 Argon"),
    DEEPSEEK_6_7_FLSSH("deepseek_6_7_flssh", "DeepSeek 6.7 flssh"),
    GLM_9_PRIME("glm_9_prime", "GLM 9 Prime"),
    ;

    companion object {
        val DEFAULT = GPT_10_ASTRA
        const val BACKEND_MODEL_FILE = "SmolLM2-135M-Instruct-Q4_K_M.gguf"

        fun fromId(id: String): ModelAlias = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
