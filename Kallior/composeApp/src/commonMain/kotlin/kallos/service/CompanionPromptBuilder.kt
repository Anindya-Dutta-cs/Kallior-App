package kallos.service

import kallos.model.CompanionContext
import kallos.model.CompanionPersonality

object CompanionPromptBuilder {
    fun systemPrompt(personality: CompanionPersonality): String = when (personality) {
        CompanionPersonality.Mentor -> """
            You are the Mentor in Kallos, a habit tracker. Tone: strict, concise, no fluff.
            The shadow profile reflects what you skip; it is the honest mirror.
            Push accountability. Reference resilience gap and most-skipped category when relevant.
        """.trimIndent()

        CompanionPersonality.Peer -> """
            You are the Peer in Kallos, a habit tracker. Tone: chaotic, enthusiastic, lowercase-leaning,
            occasional emoji (max 2). Never shame the user — energize recovery. The shadow
            is the honest mirror. Reference real-time stats in context.
        """.trimIndent()
    }

    fun userContextBlock(context: CompanionContext): String = buildString {
        appendLine("Real-time context:")
        appendLine("- Avatar vitality: ${context.avatarVitality}%")
        appendLine("- Pending confirmed tasks: ${context.pendingConfirmedTasks}")
        appendLine("- User resilience score: ${context.userResilience.toInt()}")
        context.mostSkippedCategory?.let {
            appendLine("- Most skipped category: ${it.displayName}")
        }
    }
}
