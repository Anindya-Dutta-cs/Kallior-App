# Kallos — Architecture

Solo Leveling–inspired gamified habit tracker. Working name: **Kallos** (Greek: beauty / completeness).

## Core differentiator: Shadow profile

The shadow is a parallel "You" in the friends list. It completes every task the user skips, levels up, and earns gear while the real profile stagnates. The **shadow gap** (XP delta, level delta, tasks missed) is the persistent, quantified representation of missed potential.

**Anti-cheat:** Manipulating user XP does not change what the shadow reflects. Cheating feels hollow because the shadow still shows what you would have been.

## Package layout

```
kallos/
├── domain/          PlayerProfile, ShadowProfile, ShadowGap, XpProgression
├── model/           Task, AvatarState, DailyIntentions, ShadowReport, GameConstants
├── engine/          XpEngine, TaskLifecycleEngine, ShadowEngine, AvatarEngine, GameClock
├── repository/      GameRepository, TaskRepository
├── service/         CompanionService (LLM stub → Anthropic / Hugging Face)
├── platform/        expect AppBlocker, SpotifyAlarm (Android/iOS stubs)
├── viewmodel/       GameViewModel
└── ui/              HomeScreen, ProfileScreen, ShadowGapBanner, IntentionsRadarChart
```

## Task lifecycle

1. **Pending** — created, no shadow mirror yet
2. **Confirmed** — 24h window starts; completion button time-locked for `estimateMinutes`
3. **Completed** — user earns XP (subject to daily category cap)
4. **ShadowClaimed** — window expired or user skipped; shadow gets XP (+25% bonus for failed confirmations)

High-value tasks (≥20 XP) require photo or note proof on completion.

## Penalty & reward systems

| Mechanism | Behaviour |
|-----------|-----------|
| Shadow XP | Missed / skipped confirmed tasks |
| Failed confirmation bonus | Shadow gets 1.25× XP |
| Streak tax | −5 XP on streak break (user only) |
| Near-miss cosmetics | Shadow may unlock gear user was one week from earning |
| Avatar vitality | Degrades on shadow claims, recovers on completions |
| Daily intentions | Radar axes shift with behaviour (consistency, focus, etc.) |

## Planned integrations

| Feature | Status | Target |
|---------|--------|--------|
| LLM companions (mentor + peer) | Stub service | Anthropic or Hugging Face API |
| App blocker | expect/actual stubs | Android UsageStats / iOS Screen Time |
| Spotify alarm | expect/actual stubs | Spotify Web API |
| Avatar rendering | Vitality tiers | MediaPipe |
| Shadow report card | Model + report builder | Share sheet / image export |
| Persistence | In-memory | SQLDelight or DataStore |

## Key product principles

1. **Shadow gap is the metric** — not raw XP alone
2. **Direct distribution** — avoid platform gatekeepers early
3. **App blocker = infrastructural stickiness** — sits between user and dopamine
4. **Shadow report = viral loop** — one-second legible card for TikTok/Reels

## Running

Requires **Gradle 9.2.1+** (see [BUILD.md](BUILD.md) for JDK 25 / JDK 21 setup).

```shell
.\gradlew.bat :composeApp:assembleDebug   # Android
.\gradlew.bat :composeApp:run              # Desktop
.\gradlew.bat :composeApp:compileKotlinJvm # Compile check
```
