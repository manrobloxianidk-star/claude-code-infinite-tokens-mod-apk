#include <jni.h>
#include <llama.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <unordered_map>
#include <unistd.h>
#include <vector>

namespace {

struct EngineState {
    llama_model *model = nullptr;
    llama_context *context = nullptr;
    std::atomic_bool cancelled{false};
    std::mutex generationMutex;

    ~EngineState() {
        if (context != nullptr) llama_free(context);
        if (model != nullptr) llama_model_free(model);
    }
};

std::mutex g_statesMutex;
std::unordered_map<jlong, std::shared_ptr<EngineState>> g_states;
std::atomic<jlong> g_nextHandle{1};
std::once_flag g_backendInit;

std::shared_ptr<EngineState> findState(jlong handle) {
    std::lock_guard<std::mutex> lock(g_statesMutex);
    const auto found = g_states.find(handle);
    return found == g_states.end() ? nullptr : found->second;
}

void throwIllegalState(JNIEnv *env, const char *message) {
    if (env->ExceptionCheck()) return;
    jclass exceptionClass = env->FindClass("java/lang/IllegalStateException");
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message);
        env->DeleteLocalRef(exceptionClass);
    }
}

std::string javaStringToUtf8(JNIEnv *env, jstring value) {
    if (value == nullptr) return {};
    const jsize length = env->GetStringLength(value);
    const jchar *chars = env->GetStringChars(value, nullptr);
    if (chars == nullptr) return {};

    std::string result;
    result.reserve(static_cast<size_t>(length) * 3);
    for (jsize index = 0; index < length; ++index) {
        uint32_t codePoint = chars[index];
        if (codePoint >= 0xD800 && codePoint <= 0xDBFF && index + 1 < length) {
            const uint32_t low = chars[index + 1];
            if (low >= 0xDC00 && low <= 0xDFFF) {
                codePoint = 0x10000 + ((codePoint - 0xD800) << 10) + (low - 0xDC00);
                ++index;
            } else {
                codePoint = 0xFFFD;
            }
        } else if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
            codePoint = 0xFFFD;
        }

        if (codePoint <= 0x7F) {
            result.push_back(static_cast<char>(codePoint));
        } else if (codePoint <= 0x7FF) {
            result.push_back(static_cast<char>(0xC0 | (codePoint >> 6)));
            result.push_back(static_cast<char>(0x80 | (codePoint & 0x3F)));
        } else if (codePoint <= 0xFFFF) {
            result.push_back(static_cast<char>(0xE0 | (codePoint >> 12)));
            result.push_back(static_cast<char>(0x80 | ((codePoint >> 6) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | (codePoint & 0x3F)));
        } else {
            result.push_back(static_cast<char>(0xF0 | (codePoint >> 18)));
            result.push_back(static_cast<char>(0x80 | ((codePoint >> 12) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | ((codePoint >> 6) & 0x3F)));
            result.push_back(static_cast<char>(0x80 | (codePoint & 0x3F)));
        }
    }

    env->ReleaseStringChars(value, chars);
    return result;
}

size_t utf8SequenceLength(unsigned char first) {
    if (first <= 0x7F) return 1;
    if (first >= 0xC2 && first <= 0xDF) return 2;
    if (first >= 0xE0 && first <= 0xEF) return 3;
    if (first >= 0xF0 && first <= 0xF4) return 4;
    return 0;
}

bool validUtf8Sequence(const std::string &text, size_t offset, size_t count) {
    if (count == 0 || offset + count > text.size()) return false;
    const auto first = static_cast<unsigned char>(text[offset]);
    for (size_t index = 1; index < count; ++index) {
        if ((static_cast<unsigned char>(text[offset + index]) & 0xC0) != 0x80) return false;
    }
    if (count == 3) {
        const auto second = static_cast<unsigned char>(text[offset + 1]);
        if ((first == 0xE0 && second < 0xA0) || (first == 0xED && second >= 0xA0)) return false;
    } else if (count == 4) {
        const auto second = static_cast<unsigned char>(text[offset + 1]);
        if ((first == 0xF0 && second < 0x90) || (first == 0xF4 && second >= 0x90)) return false;
    }
    return true;
}

size_t completeUtf8Prefix(const std::string &text) {
    size_t offset = 0;
    while (offset < text.size()) {
        const size_t count = utf8SequenceLength(static_cast<unsigned char>(text[offset]));
        if (count == 0) {
            ++offset;
            continue;
        }
        if (offset + count > text.size()) break;
        offset += validUtf8Sequence(text, offset, count) ? count : 1;
    }
    return offset;
}

jstring utf8ToJavaString(JNIEnv *env, const std::string &text) {
    std::vector<jchar> utf16;
    utf16.reserve(text.size());
    size_t offset = 0;
    while (offset < text.size()) {
        const auto first = static_cast<unsigned char>(text[offset]);
        const size_t count = utf8SequenceLength(first);
        uint32_t codePoint = 0xFFFD;
        if (count != 0 && validUtf8Sequence(text, offset, count)) {
            if (count == 1) {
                codePoint = first;
            } else if (count == 2) {
                codePoint = ((first & 0x1F) << 6) | (static_cast<unsigned char>(text[offset + 1]) & 0x3F);
            } else if (count == 3) {
                codePoint = ((first & 0x0F) << 12)
                    | ((static_cast<unsigned char>(text[offset + 1]) & 0x3F) << 6)
                    | (static_cast<unsigned char>(text[offset + 2]) & 0x3F);
            } else {
                codePoint = ((first & 0x07) << 18)
                    | ((static_cast<unsigned char>(text[offset + 1]) & 0x3F) << 12)
                    | ((static_cast<unsigned char>(text[offset + 2]) & 0x3F) << 6)
                    | (static_cast<unsigned char>(text[offset + 3]) & 0x3F);
            }
            offset += count;
        } else {
            ++offset;
        }

        if (codePoint <= 0xFFFF) {
            utf16.push_back(static_cast<jchar>(codePoint));
        } else {
            codePoint -= 0x10000;
            utf16.push_back(static_cast<jchar>(0xD800 + (codePoint >> 10)));
            utf16.push_back(static_cast<jchar>(0xDC00 + (codePoint & 0x3FF)));
        }
    }
    return env->NewString(utf16.data(), static_cast<jsize>(utf16.size()));
}

bool sendTokenText(JNIEnv *env, jobject listener, jmethodID method, const std::string &text) {
    if (text.empty()) return true;
    jstring javaText = utf8ToJavaString(env, text);
    if (javaText == nullptr) return false;
    env->CallVoidMethod(listener, method, javaText);
    env->DeleteLocalRef(javaText);
    return !env->ExceptionCheck();
}

bool flushCompleteText(JNIEnv *env, jobject listener, jmethodID method, std::string &pending) {
    const size_t complete = completeUtf8Prefix(pending);
    if (complete == 0) return true;
    const bool sent = sendTokenText(env, listener, method, pending.substr(0, complete));
    pending.erase(0, complete);
    return sent;
}

bool appendTokenPiece(
    JNIEnv *env,
    const llama_vocab *vocab,
    llama_token token,
    jobject listener,
    jmethodID method,
    std::string &pending
) {
    std::vector<char> piece(128);
    int32_t length = llama_token_to_piece(vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, true);
    if (length < 0) {
        piece.resize(static_cast<size_t>(-length));
        length = llama_token_to_piece(vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, true);
    }
    if (length < 0) return false;
    pending.append(piece.data(), static_cast<size_t>(length));
    return flushCompleteText(env, listener, method, pending);
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_dev_infinitetokens_inference_NativeLlama_loadModel(JNIEnv *env, jobject, jstring modelPath, jint contextTokens) {
    if (modelPath == nullptr) {
        throwIllegalState(env, "Model path is empty");
        return 0;
    }
    std::call_once(g_backendInit, [] { llama_backend_init(); });

    const std::string path = javaStringToUtf8(env, modelPath);
    auto state = std::make_shared<EngineState>();
    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0;
    state->model = llama_model_load_from_file(path.c_str(), modelParams);
    if (state->model == nullptr) {
        throwIllegalState(env, "llama.cpp could not load the bundled GGUF model");
        return 0;
    }

    const int cpuCount = static_cast<int>(sysconf(_SC_NPROCESSORS_ONLN));
    const uint32_t threads = static_cast<uint32_t>(std::max(1, std::min(4, cpuCount - 1)));
    llama_context_params contextParams = llama_context_default_params();
    contextParams.n_ctx = static_cast<uint32_t>(std::max(256, static_cast<int>(contextTokens)));
    contextParams.n_batch = std::min<uint32_t>(512, contextParams.n_ctx);
    contextParams.n_ubatch = std::min<uint32_t>(512, contextParams.n_ctx);
    contextParams.n_threads = threads;
    contextParams.n_threads_batch = threads;
    state->context = llama_init_from_model(state->model, contextParams);
    if (state->context == nullptr) {
        throwIllegalState(env, "llama.cpp could not initialize the model context");
        return 0;
    }

    const jlong handle = g_nextHandle.fetch_add(1);
    {
        std::lock_guard<std::mutex> lock(g_statesMutex);
        g_states.emplace(handle, std::move(state));
    }
    return handle;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_infinitetokens_inference_NativeLlama_beginGeneration(JNIEnv *, jobject, jlong handle) {
    const auto state = findState(handle);
    if (state == nullptr) return JNI_FALSE;
    state->cancelled.store(false);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_infinitetokens_inference_NativeLlama_generate(
    JNIEnv *env,
    jobject,
    jlong handle,
    jobjectArray roleArray,
    jobjectArray contentArray,
    jint maxNewTokens,
    jobject listener
) {
    const auto state = findState(handle);
    if (state == nullptr || listener == nullptr) {
        throwIllegalState(env, "The local model is not loaded");
        return;
    }
    std::unique_lock<std::mutex> generationLock(state->generationMutex);
    if (state->cancelled.load()) return;

    const jsize messageCount = env->GetArrayLength(roleArray);
    if (messageCount != env->GetArrayLength(contentArray) || messageCount == 0) {
        throwIllegalState(env, "The prompt message list is empty or malformed");
        return;
    }

    std::vector<std::string> roles;
    std::vector<std::string> contents;
    roles.reserve(static_cast<size_t>(messageCount));
    contents.reserve(static_cast<size_t>(messageCount));
    for (jsize index = 0; index < messageCount; ++index) {
        auto role = static_cast<jstring>(env->GetObjectArrayElement(roleArray, index));
        auto content = static_cast<jstring>(env->GetObjectArrayElement(contentArray, index));
        roles.push_back(javaStringToUtf8(env, role));
        contents.push_back(javaStringToUtf8(env, content));
        env->DeleteLocalRef(role);
        env->DeleteLocalRef(content);
        if (env->ExceptionCheck()) return;
    }

    jclass listenerClass = env->GetObjectClass(listener);
    const jmethodID onToken = listenerClass == nullptr ? nullptr : env->GetMethodID(listenerClass, "onToken", "(Ljava/lang/String;)V");
    if (listenerClass != nullptr) env->DeleteLocalRef(listenerClass);
    if (onToken == nullptr) {
        throwIllegalState(env, "Token listener does not expose onToken(String)");
        return;
    }

    std::vector<llama_chat_message> messages;
    messages.reserve(static_cast<size_t>(messageCount));
    size_t characterCount = 0;
    for (jsize index = 0; index < messageCount; ++index) {
        characterCount += roles[index].size() + contents[index].size();
        messages.push_back({roles[index].c_str(), contents[index].c_str()});
    }

    const char *templateText = llama_model_chat_template(state->model, nullptr);
    std::vector<char> formatted(std::max<size_t>(1024, characterCount * 2 + 512));
    int32_t formattedLength = llama_chat_apply_template(templateText, messages.data(), messages.size(), true, formatted.data(), static_cast<int32_t>(formatted.size()));
    if (formattedLength < 0) {
        throwIllegalState(env, "The model does not support its embedded chat template");
        return;
    }
    if (static_cast<size_t>(formattedLength) > formatted.size()) {
        formatted.resize(static_cast<size_t>(formattedLength));
        formattedLength = llama_chat_apply_template(templateText, messages.data(), messages.size(), true, formatted.data(), static_cast<int32_t>(formatted.size()));
    }
    if (formattedLength < 0) {
        throwIllegalState(env, "Failed to format the conversation with the model chat template");
        return;
    }

    const llama_vocab *vocab = llama_model_get_vocab(state->model);
    const int32_t requiredTokens = -llama_tokenize(vocab, formatted.data(), formattedLength, nullptr, 0, true, true);
    if (requiredTokens <= 0) {
        throwIllegalState(env, "llama.cpp could not tokenize the conversation");
        return;
    }
    std::vector<llama_token> promptTokens(static_cast<size_t>(requiredTokens));
    const int32_t promptTokenCount = llama_tokenize(vocab, formatted.data(), formattedLength, promptTokens.data(), requiredTokens, true, true);
    if (promptTokenCount < 0) {
        throwIllegalState(env, "llama.cpp could not tokenize the conversation");
        return;
    }

    const uint32_t contextSize = llama_n_ctx(state->context);
    const uint32_t generationSize = static_cast<uint32_t>(std::max(1, static_cast<int>(maxNewTokens)));
    if (static_cast<uint32_t>(promptTokenCount) + generationSize > contextSize) {
        throwIllegalState(env, "Conversation exceeds the local model context limit");
        return;
    }

    llama_memory_clear(llama_get_memory(state->context), true);
    llama_batch batch = llama_batch_init(promptTokenCount, 0, 1);
    batch.n_tokens = promptTokenCount;
    for (int32_t index = 0; index < promptTokenCount; ++index) {
        batch.token[index] = promptTokens[static_cast<size_t>(index)];
        batch.pos[index] = index;
        batch.n_seq_id[index] = 1;
        batch.seq_id[index][0] = 0;
        batch.logits[index] = index == promptTokenCount - 1;
    }
    if (llama_decode(state->context, batch) != 0) {
        llama_batch_free(batch);
        throwIllegalState(env, "llama.cpp failed to evaluate the conversation");
        return;
    }

    llama_sampler *sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (sampler == nullptr) {
        llama_batch_free(batch);
        throwIllegalState(env, "llama.cpp could not initialize sampling");
        return;
    }
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(40));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.92f, 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.8f));
    const auto seed = static_cast<uint32_t>(std::chrono::steady_clock::now().time_since_epoch().count());
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(seed));

    std::string pendingUtf8;
    int32_t position = promptTokenCount;
    for (uint32_t generated = 0; generated < generationSize && !state->cancelled.load(); ++generated) {
        const llama_token token = llama_sampler_sample(sampler, state->context, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        if (!appendTokenPiece(env, vocab, token, listener, onToken, pendingUtf8)) break;
        if (state->cancelled.load()) break;

        batch.n_tokens = 1;
        batch.token[0] = token;
        batch.pos[0] = position++;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;
        if (llama_decode(state->context, batch) != 0) {
            llama_sampler_free(sampler);
            llama_batch_free(batch);
            throwIllegalState(env, "llama.cpp failed while generating a response");
            return;
        }
    }

    if (!pendingUtf8.empty() && !env->ExceptionCheck()) sendTokenText(env, listener, onToken, pendingUtf8);
    llama_sampler_free(sampler);
    llama_batch_free(batch);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_infinitetokens_inference_NativeLlama_cancel(JNIEnv *, jobject, jlong handle) {
    const auto state = findState(handle);
    if (state != nullptr) state->cancelled.store(true);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_infinitetokens_inference_NativeLlama_close(JNIEnv *, jobject, jlong handle) {
    std::shared_ptr<EngineState> state;
    {
        std::lock_guard<std::mutex> lock(g_statesMutex);
        const auto found = g_states.find(handle);
        if (found == g_states.end()) return;
        state = found->second;
        g_states.erase(found);
    }
    state->cancelled.store(true);
    // An active JNI call keeps its own shared reference and frees the context/model after it exits.
}
