package kallos.service

import kallos.model.CompanionContext
import kallos.model.CompanionPersonality

/**
 * LLM-backed companions with [StubCompanionService] fallback when offline or unconfigured.
 */
class LlmCompanionService(
    private val config: LlmConfig = LlmConfig(),
    private val client: LlmClient? = llmClientFromConfig(config),
    private val fallback: CompanionService = StubCompanionService(),
) : CompanionService {
    override suspend fun respond(
        personality: CompanionPersonality,
        context: CompanionContext,
        userMessage: String,
    ): String {
        val live = client ?: return fallback.respond(personality, context, userMessage)
        val system = CompanionPromptBuilder.systemPrompt(personality)
        val contextBlock = CompanionPromptBuilder.userContextBlock(context)
        val combinedUser = buildString {
            appendLine(contextBlock)
            if (userMessage.isNotBlank()) {
                appendLine()
                appendLine("User message:")
                appendLine(userMessage)
            }
        }
        return try {
            live.complete(system, combinedUser)
        } catch (_: Exception) {
            fallback.respond(personality, context, userMessage)
        }
    }
}
