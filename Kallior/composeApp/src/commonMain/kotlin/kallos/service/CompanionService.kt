package kallos.service

import kallos.model.CompanionContext
import kallos.model.CompanionPersonality

interface CompanionService {
    suspend fun respond(
        personality: CompanionPersonality,
        context: CompanionContext,
        userMessage: String,
    ): String
}

class StubCompanionService : CompanionService {
    override suspend fun respond(
        personality: CompanionPersonality,
        context: CompanionContext,
        userMessage: String,
    ): String = when (personality) {
        CompanionPersonality.Mentor -> buildMentorReply(context, userMessage)
        CompanionPersonality.Peer -> buildPeerReply(context, userMessage)
    }

    private fun buildMentorReply(context: CompanionContext, userMessage: String): String {
        if (userMessage.isNotBlank()) {
            return "Acknowledged. Focus on execution: ${context.pendingConfirmedTasks} confirmed task(s) remain."
        }
        val skip = context.mostSkippedCategory?.displayName ?: "your habits"
        return when {
            context.shadowGap.shadowAhead ->
                "Your shadow leads by ${context.shadowGap.resilienceGap.toInt()} resilience points. $skip is where you keep yielding ground. Confirm fewer tasks than you intend to finish."
            context.pendingConfirmedTasks > 0 ->
                "You have ${context.pendingConfirmedTasks} confirmed task(s) awaiting action. The window does not wait."
            else ->
                "Maintain discipline before comfort."
        }
    }

    private fun buildPeerReply(context: CompanionContext, userMessage: String): String {
        if (userMessage.isNotBlank()) {
            return "omg yes okay — resilience gap is ${context.shadowGap.resilienceGap.toInt()} rn, we can literally close that today 💪"
        }
        return when {
            context.shadowGap.resilienceGap >= 25 ->
                "OKAY so your shadow is literally eating your lunch on ${context.mostSkippedCategory?.displayName ?: "everything"} 😭 we can fix this!!"
            context.avatarVitality < 40 ->
                "your avatar looks ROUGH rn ngl but like that's literally just consistency coming back to bite — one win today and it'll perk up I swear"
            else ->
                "shadow's resilience is way ahead — the gap is ${context.shadowGap.resilienceGap.toInt()} which is like… one solid day?? let's GO"
        }
    }
}
