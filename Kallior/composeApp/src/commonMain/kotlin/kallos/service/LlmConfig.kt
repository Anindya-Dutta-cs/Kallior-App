package kallos.service

enum class LlmProvider {
    Stub,
    Anthropic,
    HuggingFace,
}

data class LlmConfig(
    val provider: LlmProvider = LlmProvider.Stub,
    val apiKey: String? = null,
    val modelId: String? = null,
) {
    val resolvedModelId: String
        get() = modelId ?: when (provider) {
            LlmProvider.Anthropic -> "claude-sonnet-4-20250514"
            LlmProvider.HuggingFace -> "meta-llama/Llama-3.1-8B-Instruct"
            LlmProvider.Stub -> ""
        }

    val isLive: Boolean
        get() = provider != LlmProvider.Stub && !apiKey.isNullOrBlank()
}
