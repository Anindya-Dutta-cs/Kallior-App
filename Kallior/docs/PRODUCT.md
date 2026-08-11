# Kallos — Product specification

**Working name:** Kallos (Solo Leveling–inspired habit RPG)  
**Stack:** Kotlin Multiplatform (Android + iOS), Anthropic or Hugging Face LLM, Spotify API, MediaPipe (future avatar rendering)

## Core differentiator: Shadow profile

A parallel **You** in the friends list that completes every task the user skips. The shadow levels up, earns gear, and stays vital while the real profile stagnates. **Shadow gap** (XP and level delta) is the persistent, quantified cost of missed potential.

### Anti-cheat principle

Manipulating player XP does not change what the shadow reflects. The shadow only gains XP from confirmed tasks that expire, are skipped, or fail the 24-hour window. Cheating feels hollow because the gap remains honest.

## Feature map → code

| Feature | Status | Location |
|--------|--------|----------|
| Task + confirmation (24h window) | Implemented | `Task`, `TaskLifecycleEngine` |
| Time-locked complete button | Implemented | `TaskLifecycleEngine.canComplete` |
| Proof for high-value tasks | Implemented | `CompletionProof`, `PROOF_REQUIRED_XP_THRESHOLD` |
| Daily XP caps per category | Implemented | `XpEngine`, `GameConstants` |
| Shadow claims + bonus XP | Implemented | `ShadowEngine`, `FAILED_CONFIRMED_SHADOW_BONUS_MULTIPLIER` |
| Streak tax | Implemented | `XpEngine.applyStreakBreakTax` |
| Avatar vitality / degradation | Implemented | `AvatarState`, `AvatarEngine` |
| Weekly streak cosmetics | Implemented | `Cosmetic`, `AvatarEngine.onStreakExtended` |
| Shadow near-miss cosmetics | Implemented | `ShadowProfile.tryClaimNearMissCosmetic` |
| Daily intentions radar | Implemented | `DailyIntentions`, `IntentionsRadarChart` |
| Shadow gap UI | Implemented | `ShadowGapBanner` |
| LLM companions (mentor + peer) | Stub + prompt layer | `LlmCompanionService`, `CompanionPromptBuilder` |
| Weekly shadow report card | Implemented | `ShadowReport`, `ShadowReportCard` |
| App blocker (paid unlock) | expect/actual stub | `platform/AppBlocker` |
| Spotify alarm on dismiss | expect/actual stub | `platform/SpotifyAlarm` |
| MediaPipe avatar | Not started | — |
| Persistence / auth / social | Not started | `GameRepository` in-memory |

## Game loop

1. **Create** task (pending) — shadow does not mirror yet.
2. **Confirm** — starts 24h window + time-lock for completion button.
3. **Complete** (after lock, with proof if required) — player XP, avatar recovery, intentions bump.
4. **Skip or expire** — shadow XP (+25% bonus on failed confirmation), avatar penalty, possible near-miss cosmetic steal.

## Viral loop

**Weekly shadow report** — single-glance card (avatar vs shadow, XP gap, divergence moment) designed for TikTok/Reels screenshots. See `ShadowReportScreen`.

## Distribution

Direct distribution (APK / TestFlight) preferred over store gatekeepers for early stage.

## Principles

1. Shadow is product and anti-cheat.
2. App blocker = infrastructural stickiness between user and dopamine.
3. Shadow report = shareable, slightly painful, legible in one second.
