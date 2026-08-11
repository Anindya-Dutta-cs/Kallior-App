package kallos.service

/**
 * Platform-neutral LLM transport. Wire HTTP in a future pass (Ktor or expect/actual).
 */
interface LlmClient {
    suspend fun complete(systemPrompt: String, userMessage: String): String
}

class AnthropicLlmClient(
    private val apiKey: String,
    private val model: String,
) : LlmClient {
    override suspend fun complete(systemPrompt: String, userMessage: String): String {
        throw UnsupportedOperationException(
            "Anthropic Messages API not wired yet. Use LlmProvider.Stub or implement HTTP.",
        )
    }
}

class HuggingFaceLlmClient(
    private val apiKey: String,
    private val model: String,
) : LlmClient {
    override suspend fun complete(systemPrompt: String, userMessage: String): String {
        throw UnsupportedOperationException(
            "Hugging Face Inference API not wired yet. Use LlmProvider.Stub or implement HTTP.",
        )
    }
}

fun llmClientFromConfig(config: LlmConfig): LlmClient? {
    if (!config.isLive) return null
    return when (config.provider) {
        LlmProvider.Anthropic -> AnthropicLlmClient(config.apiKey!!, config.resolvedModelId)
        LlmProvider.HuggingFace -> HuggingFaceLlmClient(config.apiKey!!, config.resolvedModelId)
        LlmProvider.Stub -> null
    }
}
