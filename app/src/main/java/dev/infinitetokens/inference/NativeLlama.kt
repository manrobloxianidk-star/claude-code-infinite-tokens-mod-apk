package dev.infinitetokens.inference

object NativeLlama {
    init {
        System.loadLibrary("infinite_tokens")
    }

    external fun loadModel(path: String, contextTokens: Int): Long
    external fun beginGeneration(handle: Long): Boolean
    external fun countPromptTokens(handle: Long, roles: Array<String>, contents: Array<String>): Int
    external fun generate(
        handle: Long,
        roles: Array<String>,
        contents: Array<String>,
        maxNewTokens: Int,
        listener: TokenListener,
    )
    external fun cancel(handle: Long)
    external fun close(handle: Long)
}

fun interface TokenListener {
    fun onToken(text: String)
}
