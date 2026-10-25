package dev.infinitetokens.inference

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.infinitetokens.model.PromptMessage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeInferenceTest {
    @Test
    fun bundledModelLoadsAndGeneratesText() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val modelFile = ModelAssetInstaller(context).installIfNeeded()
        val engine = LlamaCppInferenceEngine()

        try {
            engine.load(modelFile)
            val response = withTimeout(180_000) {
                engine.generate(
                    messages = listOf(PromptMessage("user", "Reply with one short word.")),
                    maxNewTokens = 16,
                ).toList().joinToString("")
            }
            assertTrue("The inference engine returned no response", response.isNotBlank())
        } finally {
            engine.close()
        }
    }
}
