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

    fun hasInstalledModel(): Boolean {
        val modelFile = File(File(appContext.filesDir, "models"), MODEL_FILE_NAME)
        return modelFile.isFile && modelFile.length() == MODEL_SIZE_BYTES
    }

    suspend fun installIfNeeded(
        onProgress: (ModelInstallProgress) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val modelDirectory = File(appContext.filesDir, "models")
        if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
            throw IOException("Could not create private model directory")
        }

        val modelFile = File(modelDirectory, MODEL_FILE_NAME)
        if (modelFile.exists()) {
            if (modelFile.isFile && modelFile.length() == MODEL_SIZE_BYTES && sha256(modelFile, onProgress) == EXPECTED_SHA256) {
                return@withContext modelFile
            }
            if (!modelFile.delete()) throw IOException("Could not replace the invalid local model file")
        }

        val partialFile = File(modelDirectory, "$MODEL_FILE_NAME.partial")
        partialFile.delete()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            appContext.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(partialFile).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    var copied = 0L
                    var lastReported = 0L
                    onProgress(ModelInstallProgress(copied, MODEL_SIZE_BYTES))
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        copied += read
                        if (copied - lastReported >= PROGRESS_INTERVAL_BYTES || copied == MODEL_SIZE_BYTES) {
                            onProgress(ModelInstallProgress(copied, MODEL_SIZE_BYTES))
                            lastReported = copied
                        }
                    }
                    output.fd.sync()
                }
            }

            if (partialFile.length() != MODEL_SIZE_BYTES) {
                throw IOException("Bundled model has an unexpected size")
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualHash != EXPECTED_SHA256) {
                throw IOException("Bundled model checksum did not match")
            }
            if (!partialFile.renameTo(modelFile)) {
                throw IOException("Could not move the verified model into private storage")
            }
            onProgress(ModelInstallProgress(MODEL_SIZE_BYTES, MODEL_SIZE_BYTES))
            modelFile
        } catch (error: Exception) {
            partialFile.delete()
            throw error
        }
    }

    private fun sha256(file: File, onProgress: (ModelInstallProgress) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val total = file.length()
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            var processed = 0L
            var lastReported = 0L
            onProgress(ModelInstallProgress(0L, total))
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                processed += read
                if (processed - lastReported >= PROGRESS_INTERVAL_BYTES || processed == total) {
                    onProgress(ModelInstallProgress(processed, total))
                    lastReported = processed
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val ASSET_PATH = "models/SmolLM2-135M-Instruct-Q4_K_M.gguf"
        private const val MODEL_FILE_NAME = "SmolLM2-135M-Instruct-Q4_K_M.gguf"
        const val MODEL_SIZE_BYTES = 105_454_432L
        val MODEL_SIZE_MEGABYTES: Int = ((MODEL_SIZE_BYTES + MEBIBYTE - 1) / MEBIBYTE).toInt()
        private const val EXPECTED_SHA256 = "2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d"
        private const val COPY_BUFFER_SIZE = 128 * 1024
        private const val PROGRESS_INTERVAL_BYTES = 1024 * 1024L
        private const val MEBIBYTE = 1024 * 1024L
    }
}
