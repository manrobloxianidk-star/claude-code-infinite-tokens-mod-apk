package dev.infinitetokens.inference

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ModelInstallProgress(
    val copiedBytes: Long,
    val totalBytes: Long,
)

class ModelAssetInstaller(context: Context) {
    private val appContext = context.applicationContext

    suspend fun installIfNeeded(
        onProgress: (ModelInstallProgress) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val modelDirectory = File(appContext.filesDir, "models")
        if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
            throw IOException("Could not create private model directory")
        }

        val modelFile = File(modelDirectory, MODEL_FILE_NAME)
        if (modelFile.isFile && modelFile.length() == EXPECTED_BYTES) return@withContext modelFile

        val partialFile = File(modelDirectory, "$MODEL_FILE_NAME.partial")
        partialFile.delete()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            appContext.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(partialFile).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    var copied = 0L
                    onProgress(ModelInstallProgress(copied, EXPECTED_BYTES))
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copied += read
                        onProgress(ModelInstallProgress(copied, EXPECTED_BYTES))
                    }
                    output.fd.sync()
                }
            }

            if (partialFile.length() != EXPECTED_BYTES) {
                throw IOException("Bundled model has an unexpected size")
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualHash != EXPECTED_SHA256) {
                throw IOException("Bundled model checksum did not match")
            }
            if (!partialFile.renameTo(modelFile)) {
                throw IOException("Could not move the verified model into private storage")
            }
            onProgress(ModelInstallProgress(EXPECTED_BYTES, EXPECTED_BYTES))
            modelFile
        } catch (error: Exception) {
            partialFile.delete()
            throw error
        }
    }

    private companion object {
        const val ASSET_PATH = "models/SmolLM2-135M-Instruct-Q4_K_M.gguf"
        const val MODEL_FILE_NAME = "SmolLM2-135M-Instruct-Q4_K_M.gguf"
        const val EXPECTED_BYTES = 105_454_432L
        const val EXPECTED_SHA256 = "2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d"
        const val COPY_BUFFER_SIZE = 128 * 1024
    }
}
