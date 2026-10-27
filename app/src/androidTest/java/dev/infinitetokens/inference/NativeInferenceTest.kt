package dev.infinitetokens.inference

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import dev.infinitetokens.model.ChaosLevel
import dev.infinitetokens.model.PromptMessage
import dev.infinitetokens.model.PromptBuilder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeInferenceTest {
    @Test
    fun downloadedModelLoadsAndGeneratesText() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testModelDirectory = File(context.cacheDir, "native-inference-model")
        val modelFile = withTimeout(300_000) {
            ModelDownloader(context, testModelDirectory).downloadIfNeeded()
        }
        assertTrue("The downloaded model has an unexpected size", modelFile.length() == ModelDownloader.MODEL_SIZE_BYTES)
        val engine = LlamaCppInferenceEngine()

        try {
            engine.load(modelFile)
            val response = withTimeout(180_000) {
                engine.generate(
                    messages = listOf(
                        PromptMessage("system", PromptBuilder.systemPrompt(ChaosLevel.ULTCOD)),
                        PromptMessage("user", "Скажи привет одним словом."),
                    ),
                    maxNewTokens = 16,
                ).toList().joinToString("")
            }
            assertTrue("The inference engine returned no response", response.isNotBlank())
            assertTrue("Russian replies should open with a curse", response.startsWith("Блядь"))
        } finally {
            engine.close()
        }
    }
}
