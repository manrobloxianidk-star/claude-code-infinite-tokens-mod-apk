package dev.infinitetokens.inference

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class ModelDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
)

class ModelDownloader(
    context: Context,
    private val modelDirectory: File = File(context.applicationContext.filesDir, "models"),
) {
    fun hasInstalledModel(): Boolean {
        val modelFile = File(modelDirectory, MODEL_FILE_NAME)
        return modelFile.isFile && modelFile.length() == MODEL_SIZE_BYTES
    }

    suspend fun downloadIfNeeded(
        onProgress: (ModelDownloadProgress) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
            throw IOException("Could not create private model directory")
        }

        val modelFile = File(modelDirectory, MODEL_FILE_NAME)
        if (modelFile.exists()) {
            if (modelFile.isFile && modelFile.length() == MODEL_SIZE_BYTES && sha256(modelFile, onProgress) == EXPECTED_SHA256) {
                return@withContext modelFile
            }
            if (!modelFile.delete()) throw IOException("Could not replace the invalid model file")
        }

        val partialFile = File(modelDirectory, "$MODEL_FILE_NAME.partial")
        partialFile.delete()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val connection = (URL(MODEL_DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IOException("Model download failed with HTTP $responseCode")
                }
                val responseLength = connection.contentLengthLong
                if (responseLength > 0 && responseLength != MODEL_SIZE_BYTES) {
                    throw IOException("The model download has an unexpected size")
                }

                var downloaded = 0L
                var lastReported = 0L
                onProgress(ModelDownloadProgress(downloaded, MODEL_SIZE_BYTES))
                connection.inputStream.buffered().use { input ->
                    FileOutputStream(partialFile).use { rawOutput ->
                        val output = rawOutput.buffered()
                        val buffer = ByteArray(COPY_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (downloaded + read > MODEL_SIZE_BYTES) {
                                throw IOException("The model download exceeds its expected size")
                            }
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            if (downloaded - lastReported >= PROGRESS_INTERVAL_BYTES || downloaded == MODEL_SIZE_BYTES) {
                                onProgress(ModelDownloadProgress(downloaded, MODEL_SIZE_BYTES))
                                lastReported = downloaded
                            }
                        }
                        output.flush()
                        rawOutput.fd.sync()
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (partialFile.length() != MODEL_SIZE_BYTES) {
                throw IOException("The model download is incomplete")
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualHash != EXPECTED_SHA256) {
                throw IOException("The model download checksum did not match")
            }
            if (!partialFile.renameTo(modelFile)) {
                throw IOException("Could not move the verified model into private storage")
            }
            onProgress(ModelDownloadProgress(MODEL_SIZE_BYTES, MODEL_SIZE_BYTES))
            modelFile
        } catch (error: Exception) {
            partialFile.delete()
            throw error
        }
    }

    private fun sha256(file: File, onProgress: (ModelDownloadProgress) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val total = file.length()
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            var processed = 0L
            var lastReported = 0L
            onProgress(ModelDownloadProgress(0L, total))
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                processed += read
                if (processed - lastReported >= PROGRESS_INTERVAL_BYTES || processed == total) {
                    onProgress(ModelDownloadProgress(processed, total))
                    lastReported = processed
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MODEL_URL = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/d255afaffd3441b95abca9b5cc4c819b93f66936/SmolLM2-135M-Instruct-Q4_K_M.gguf?download=true"
        private const val MODEL_FILE_NAME = "SmolLM2-135M-Instruct-Q4_K_M.gguf"
        const val MODEL_SIZE_BYTES = 105_454_432L
        val MODEL_SIZE_MEGABYTES: Int = ((MODEL_SIZE_BYTES + 999_999L) / 1_000_000L).toInt()
        private const val EXPECTED_SHA256 = "2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d"
        private const val COPY_BUFFER_SIZE = 128 * 1024
        private const val PROGRESS_INTERVAL_BYTES = 1024 * 1024L
        private const val CONNECT_TIMEOUT_MILLIS = 20_000
        private const val READ_TIMEOUT_MILLIS = 60_000
        private const val USER_AGENT = "ClaudeAndroid/0.1.9"
    }
}
