package dev.infinitetokens.inference

import dev.infinitetokens.model.PromptMessage
import dev.infinitetokens.model.AngryVoice
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface LocalInferenceEngine {
    suspend fun load(modelFile: File)
    suspend fun countPromptTokens(messages: List<PromptMessage>): Int
    fun generate(messages: List<PromptMessage>, maxNewTokens: Int = 128): Flow<String>
    fun cancel()
    fun close()
}

class LlamaCppInferenceEngine(
    private val contextTokens: Int = 4096,
) : LocalInferenceEngine {
    private data class GenerationStart(val handle: Long?, val error: String?)

    private val nativeHandle = AtomicLong(0L)
    private val loadingMutex = Mutex()
    private val generationActive = AtomicBoolean(false)
    private val nativeControlLock = Any()
    @Volatile
    private var closed = false

    override suspend fun load(modelFile: File) = withContext(Dispatchers.IO) {
        loadingMutex.withLock {
            synchronized(nativeControlLock) {
                if (closed) throw CancellationException("The local inference engine is closed")
            }
            if (nativeHandle.get() != 0L) return@withLock
            if (!modelFile.exists() || !modelFile.isFile || !modelFile.canRead()) {
                throw IOException("Local model file is missing or unreadable")
            }
            val handle = NativeLlama.loadModel(modelFile.absolutePath, contextTokens)
            if (handle == 0L) throw IOException("llama.cpp could not load the local model")
            try {
                val coroutineContext = currentCoroutineContext()
                coroutineContext.ensureActive()
                synchronized(nativeControlLock) {
                    coroutineContext.ensureActive()
                    if (closed) throw CancellationException("The local inference engine is closed")
                    nativeHandle.set(handle)
                }
            } catch (error: Throwable) {
                NativeLlama.close(handle)
                throw error
            }
        }
    }

    override suspend fun countPromptTokens(messages: List<PromptMessage>): Int = withContext(Dispatchers.IO) {
        val handle = nativeHandle.get()
        if (handle == 0L) throw IOException("The local model has not been loaded")
        NativeLlama.countPromptTokens(
            handle,
            messages.map(PromptMessage::role).toTypedArray(),
            messages.map(PromptMessage::content).toTypedArray(),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun generate(messages: List<PromptMessage>, maxNewTokens: Int): Flow<String> = callbackFlow {
        if (messages.isEmpty()) {
            close(IllegalArgumentException("At least one prompt message is required"))
            return@callbackFlow
        }

        val start = synchronized(nativeControlLock) {
            val handle = nativeHandle.get()
            when {
                handle == 0L -> GenerationStart(null, "The local model has not been loaded")
                !generationActive.compareAndSet(false, true) ->
                    GenerationStart(null, "A response is already being generated")
                NativeLlama.beginGeneration(handle) -> GenerationStart(handle, null)
                else -> {
                    generationActive.set(false)
                    GenerationStart(null, "The local model was closed")
                }
            }
        }
        val handle = start.handle
        if (handle == null) {
            close(IllegalStateException(start.error ?: "Could not start local generation"))
            return@callbackFlow
        }

        val worker = launch(Dispatchers.IO) {
            try {
                val latestUserMessage = messages.lastOrNull { it.role == "user" }?.content.orEmpty()
                val firstReplyText = StringBuilder()
                var decoratedFirstText = false
                NativeLlama.generate(
                    handle = handle,
                    roles = messages.map(PromptMessage::role).toTypedArray(),
                    contents = messages.map(PromptMessage::content).toTypedArray(),
                    maxNewTokens = maxNewTokens.coerceIn(1, 1024),
                    listener = TokenListener { text ->
                        val visibleText = if (decoratedFirstText) {
                            text
                        } else {
                            firstReplyText.append(text)
                            if (firstReplyText.isBlank()) {
                                ""
                            } else {
                                decoratedFirstText = true
                                AngryVoice.prefix(firstReplyText.toString(), latestUserMessage).also { firstReplyText.setLength(0) }
                            }
                        }
                        if (visibleText.isNotEmpty() && trySendBlocking(visibleText).isFailure) NativeLlama.cancel(handle)
                    },
                )
                this@callbackFlow.close()
            } catch (error: Throwable) {
                this@callbackFlow.close(error)
            } finally {
                generationActive.set(false)
            }
        }

        awaitClose {
            if (worker.isActive) {
                NativeLlama.cancel(handle)
                worker.cancel()
            }
        }
    }

    override fun cancel() {
        synchronized(nativeControlLock) {
            nativeHandle.get().takeIf { it != 0L }?.let(NativeLlama::cancel)
        }
    }

    override fun close() {
        synchronized(nativeControlLock) {
            closed = true
            nativeHandle.get().takeIf { it != 0L }?.let(NativeLlama::cancel)
            nativeHandle.getAndSet(0L).takeIf { it != 0L }?.let(NativeLlama::close)
        }
    }
}
