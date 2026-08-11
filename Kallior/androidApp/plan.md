# SHADOW OVERLAY INTEGRATION SPEC

> **For Qwen Code.** Read this document fully before the first edit. Execute steps in order, one commit per step, run `./gradlew test` (or the project's equivalent) after each step. Where this doc conflicts with existing code, this doc wins; record every conflict in `IMPLEMENTATION_NOTES.md`.

---

## 1. Objective

Tie the existing shadow backend (`kallos.engine.ShadowRadarEngine`, `kallos.domain.*`) to a new full-screen **Shadow Homescreen overlay** on the homescreen, revealed by a slider/drag gesture. The shadow represents the user's *potential*: it always mirrors the user's tasks, completes what the user completes (Case 1), and auto-completes ("claims") any task the user leaves incomplete for 6+ hours (Case 2). It must feel personal, not statistical.

Non-goals: changing the user-side radar formulas; changing the mercy system semantics; redesigning existing user homescreen visuals.

---

## 2. Verified current architecture (facts from `ShadowRadarEngine.kt`)

- `computeShadowScores(userScores, snapshots, shadow)`:
  - 4 axes (discipline, focus, health, resilience) = average of the **top 3** values from the latest `DailyMetricSnapshot`'s per-axis 7-day list, rounded to int.
  - consistency = same top-3 average **plus** `tasksClaimedCount * 2.0`, capped at `CEILING = 99.0`.
  - Mercy: all axes × `0.8` when the last 3 snapshots all have `yestAvg < 60.0`.
- `DailyMetricSnapshot` holds `consistencyList / disciplineList / focusList / healthList / resilienceList` (rolling 7-day) and `yestAvg`.
- `ShadowProfile` holds a stored `tasksClaimedCount`.

Assumed (verify; adapt names if different and note in `IMPLEMENTATION_NOTES.md`):
- A user task model with at least: `id`, `title`, `createdAt` (epoch/`Instant`), `completedAt: Instant?`.
- A Compose homescreen with Tasks / Reminders / Badges sections and a radar chart composable.
- A repository layer (Room/DataStore) exposing `tasksFlow` and `snapshotsFlow`.

---

## 3. Binding product rules

| # | Rule |
|---|------|
| R1 | Maintain rolling **7-day** lists for the 4 axes (already in snapshot; ensure a daily writer exists — Step 4). |
| R2 | Shadow 4-axis score = average of the user's **top 3** values of the respective 7-day list (unchanged behavior). |
| R3 | Shadow **consistency is computed separately**, event-driven from task completion. It no longer reads `consistencyList` and no longer uses the top-3 average. |
| R4 | **Case 1:** every task the user completed is completed on the shadow side too (mirror). |
| R5 | **Case 2:** any task still incomplete **6h after creation** is claimed by the shadow at exactly `createdAt + 6h`. This must appear in the shadow task container and in the shadow radar (consistency), and must **never** mutate the user's task record. |
| R6 | Shadow task/reminders/badges containers are **the exact same composables** as the user's (single implementation, two configurations). |
| R7 | Mercy and `CEILING = 99` still apply to all 5 shadow axes, including the new consistency. |
| R8 | Claim state is **derived purely from timestamps** — no background jobs, no stored mutation of tasks, no migrations. |

---

## 4. Design tokens (exact)

Create `ui/theme/ShadowTokens.kt` (or extend the existing theme file). **No raw hex anywhere else in the codebase.**

```kotlin
val ShadowPurple      = Color(0xFFC671FB) // icons, radar glyphs, accents
val ShadowGradientTop = Color(0xFF150322) // panel gradient start (top)
val ShadowGradientBot = Color(0xFF000000) // panel gradient end (bottom)
val ShadowButton      = Color(0xFF4E3360) // FAB / card button fill
val ShadowButtonGlyph = Color(0xFF2C2C2C) // "+" glyph
// TODO(a11y): ShadowButtonGlyph on ShadowButton is ~2.2:1 contrast (WCAG fail).
// Ship as designed; if design approves, swap glyph to ShadowPurple.
```

Gradient direction: **vertical, top → bottom**, applied to the rounded-top panel only (matches mock). Panel top corners rounded ~28dp.

---

## 5. Step 1 — Domain: shadow task state (pure derivation)

New file `kallos/engine/ShadowTaskEngine.kt`:

```kotlin
package kallos.engine

import kallos.domain.Task // adapt to real model name
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

enum class ShadowTaskState { PENDING, DONE_BY_USER, CLAIMED_BY_SHADOW }

object ShadowTaskEngine {
    val CLAIM_DELAY = 6.hours

    /** Pure: derives the shadow-side state of a task at time [now]. */
    fun stateOf(task: Task, now: Instant): ShadowTaskState {
        val threshold = task.createdAt + CLAIM_DELAY
        val userDoneInTime = task.completedAt != null && task.completedAt <= threshold
        return when {
            userDoneInTime -> ShadowTaskState.DONE_BY_USER
            now >= threshold -> ShadowTaskState.CLAIMED_BY_SHADOW
            task.completedAt != null -> ShadowTaskState.DONE_BY_USER // late user completion; shadow already claimed, still "completed"
            else -> ShadowTaskState.PENDING
        }
    }

    /** Completion timestamp from the shadow's perspective; null if pending. */
    fun shadowCompletedAt(task: Task, now: Instant): Instant? = when (stateOf(task, now)) {
        ShadowTaskState.PENDING -> null
        ShadowTaskState.DONE_BY_USER -> task.completedAt
        ShadowTaskState.CLAIMED_BY_SHADOW -> task.createdAt + CLAIM_DELAY
    }

    fun claimedCount(tasks: List<Task>, now: Instant): Int =
        tasks.count { stateOf(it, now) == ShadowTaskState.CLAIMED_BY_SHADOW }

    fun lastClaimed(tasks: List<Task>, now: Instant): Task? =
        tasks.filter { stateOf(it, now) == ShadowTaskState.CLAIMED_BY_SHADOW }
             .maxByOrNull { it.createdAt }
}
```

Boundary semantics (binding): exactly `createdAt + 6h` ⇒ claimed (`>=`). If the project uses epoch millis/`java.time`, adapt types but keep the logic identical. It should only target one task at a time. **Do not add a worker/job that flips tasks.**

---

## 6. Step 2 — Consistency: one formula, two lenses

1. Locate where `userScores.consistency` is currently computed. Extract it into `kallos/engine/ConsistencyEngine.kt` as a pure function parameterized by a completion lookup:

```kotlin
object ConsistencyEngine {
    /** [completedAtOf] is the lens: user passes task.completedAt; shadow passes ShadowTaskEngine.shadowCompletedAt. */
    fun score(tasks: List<Task>, now: Instant, completedAtOf: (Task, Instant) -> Instant?): Double
    fun userConsistency(tasks: List<Task>, now: Instant) = score(tasks, now) { t, _ -> t.completedAt }
    fun shadowConsistency(tasks: List<Task>, now: Instant) = score(tasks, now, ShadowTaskEngine::shadowCompletedAt)
}
```

2. If no single extraction point exists, refactor until one does — the user and shadow scores **must** be comparable (same formula, different lens).
3. If (and only if) no user consistency formula exists anywhere, implement this fallback for both lenses: window = last 7 days; `due` = tasks with `createdAt` in window; `done` = due tasks whose lens-completion is non-null; `score = 100.0 * done / max(1, due)`.
4. Expected behavior: because the shadow completes everything within 6h, shadow consistency trends high — **this is intended** (the shadow is potential). Mercy + ceiling keep it non-perfect.

---

## 7. Step 3 — Rewire `ShadowRadarEngine`

Change signature and consistency source; keep everything else:

```kotlin
fun computeShadowScores(
    userScores: RadarScores,
    snapshots: List<DailyMetricSnapshot>,
    tasks: List<Task>,
    now: Instant,
    shadow: ShadowProfile = ShadowProfile(),
): RadarScores {
    val mercy = if (isMercyActive(snapshots)) MERCY_MULTIPLIER else 1.0
    val consistency = ConsistencyEngine.shadowConsistency(tasks, now)
    val discipline = baseShadowValue(snapshots, { it.disciplineList }, userScores.discipline)
    val focus      = baseShadowValue(snapshots, { it.focusList },      userScores.focus)
    val health     = baseShadowValue(snapshots, { it.healthList },     userScores.health)
    val resilience = baseShadowValue(snapshots, { it.resilienceList }, userScores.resilience)
    return RadarScores(
        consistency = (consistency * mercy).coerceAtMost(CEILING),
        discipline  = (discipline  * mercy).coerceAtMost(CEILING),
        focus       = (focus       * mercy).coerceAtMost(CEILING),
        health      = (health      * mercy).coerceAtMost(CEILING),
        resilience  = (resilience  * mercy).coerceAtMost(CEILING),
    )
}
```

- **Delete** `CONSISTENCY_BOOST_PER_CLAIM` and the top-3 consistency path (double-counts claims now that consistency is event-driven).
- **Stop reading** `consistencyList` for the shadow (keep the field if the user side uses it).
- Keep `baseShadowValue`, `isMercyActive`, `CEILING` unchanged.
- `ShadowProfile.tasksClaimedCount`: deprecate the *stored* counter; the ViewModel exposes the derived `ShadowTaskEngine.claimedCount` instead. Update all callers of the old overloads; delete the 2-arg overload.

---

## 8. Step 4 — Daily snapshot writer (verify or create)

Verify a recorder exists that, on first foreground of each calendar day, appends **yesterday's** user axis scores to the four 7-day lists, trims each list to 7 entries, and sets `yestAvg` = mean of yesterday's 4 axis scores, keyed by UTC date (idempotent per day). If missing, implement `kallos.data.DailySnapshotRecorder` accordingly. Unit-test: trimming at 7, idempotency, `yestAvg` math.

---

## 9. Step 5 — ViewModel: `ShadowHomeState` + quantized time

In the existing homescreen ViewModel (single source of truth — do **not** create a second ViewModel):

```kotlin
data class ShadowHomeState(
    val scores: RadarScores,
    val tasks: List<TaskUi>,        // SAME TaskUi type as the user list
    val claimedCount: Int,
    val mercyActive: Boolean,
    val lastClaimTitle: String?,    // for microcopy
)

// Quantized clock: recompute at most once/minute + on data change. Never per frame.
private val minuteTick = flow { while (true) { emit(clock.now()); delay(60_000) } }

val shadowHomeState: StateFlow<ShadowHomeState> =
    combine(tasksFlow, snapshotsFlow, userScoresFlow, minuteTick) { tasks, snaps, user, now ->
        buildShadowState(tasks, snaps, user, now)   // O(tasks), trivial
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ShadowHomeState.EMPTY)
```

`TaskUi` gains two fields only: `shadowState: ShadowTaskState` and `shadowCompleted: Boolean` (`shadowState != PENDING`). The user-facing `isCompleted` stays untouched.

---

## 10. Step 6 — UI: extract shared section composables FIRST

Refactor the **user** homescreen so Tasks / Reminders / Badges sections (header row with "+" FAB, card container, empty-state placeholders) are shared composables parameterized by data + accent config. The user homescreen must render pixel-identical before/after this refactor (verify with screenshot test). Then the shadow page consumes the *same* composables with shadow data — this is what makes R6 true and prevents drift.

Shadow-side "+" behavior: visually identical; on tap show snackbar `shadow_add_blocked` = "The shadow mirrors you. Assign tasks on your side." (string resource).

---

## 11. Step 7 — UI: overlay page + slider mechanics

Layout (matches mock, top → bottom):
1. Handle row: two-line handle icon top-left (closes the overlay).
2. `ShadowRadarChart` (Step 8) on transparent black.
3. Rounded-top panel, vertical gradient `ShadowGradientTop → ShadowGradientBot`, containing the shared sections in a `LazyColumn` (items keyed by stable task id).

Mechanics:
- Full-screen layer above the user homescreen in a `Box`; slides **horizontally from the right**.
- Prefer Material3 `AnchoredDraggableState` with anchors `HIDDEN at widthPx`, `REVEALED at 0f`, `spring(dampingRatio = 0.85f, stiffness = 380f)`, positional threshold 50%, velocity threshold ~900f. If the project's M3 version lacks it, implement custom drag with `awaitPointerEventScope` — same anchor semantics.
- Apply offset via `Modifier.graphicsLayer { translationX = offsetX }` — **never** via recomposition.
- Discoverability: when hidden, a 24dp edge tab with a shadow glyph at the right edge starts the drag; tap toggles with spring. When revealed, the top-left handle closes; `BackHandler` closes before exiting the app.
- Hit-testing: at progress == 0 the overlay must not intercept touches.
- Composition gating: `var everRevealed by remember { mutableStateOf(false) }`; skip composing overlay content until first interaction.
- A11y: `semantics { paneTitle = "Shadow homescreen" }`, toggle action, per-axis `contentDescription` with values.

---

## 12. Step 8 — Radar chart wiring + personal microcopy

- Reuse the user radar composable parameterized by values + tint; icon/label tint = `ShadowPurple`.
- Axis order (clockwise from top): CONSISTENCY, DISCIPLINE, HEALTH, RESILIENCE, and the focus axis. The mock labels it **SKILL**; keep domain field `focus`, externalize label as string resource `shadow_axis_focus_label` = "SKILL" with a `TODO(naming)` comment. One-line rename later.
- Personal touch (recommended, cheap): one line under the radar when `lastClaimTitle != null`: `shadow_claim_line` = "The shadow claimed “%1$s” after 6 hours of silence." Also expose `claimedCount` as "N tasks claimed" in the Tasks header subtitle. This is what makes it *feel personal* — the shadow speaks in specifics, not averages.

---

## 13. Edge cases (binding)

| Case | Behavior |
|------|----------|
| Task deleted / edited | Mirrors instantly on shadow side (derived state). |
| User completes after claim | Shadow shows completed (claimed at 6h); user side shows their completion. Both happy. |
| User un-completes (if supported) | Re-derive: flips to CLAIMED if past threshold, else PENDING. |
| Recurring tasks | Each occurrence has its own `createdAt`; treat independently. |
| Clock rollback / negative durations | Clamp to 0. Store instants as UTC epoch. |
| No snapshots (new user) | 4 axes fall back to current user scores (existing `fallback` param); consistency live from tasks. |
| Sleep hours | Shadow does not sleep (default). Mercy softens scores during bad stretches. |

---

## 14. Tests (must pass before completion)

Unit:
- `ShadowTaskEngineTest`: pending before 6h; claimed at exactly 6h; `DONE_BY_USER` when completed at 5h59m; `CLAIMED_BY_SHADOW` when completed at 6h01m (shadow got there first); `claimedCount`; `lastClaimed`.
- `ConsistencyEngineTest`: same task set through both lenses; shadow ≥ user always; window math.
- `ShadowRadarEngineTest`: top-3 average per axis; mercy on/off (3-day rule); ceiling 99 on every axis; consistency no longer reads `consistencyList`; empty-snapshot fallback.
- `DailySnapshotRecorderTest`: trim to 7; idempotent per day; `yestAvg`.

UI (Compose test or screenshot):
- Tokens match hex exactly; overlay anchors hidden/revealed; hidden overlay passes touches through; back-press closes overlay first.

---

## 15. Performance & UX guardrails

- No allocation inside radar drawing; build `Path` with `remember(values)`.
- Scores recompute ≤ 1/min (Step 5 tick) — never tied to drag frames.
- Overlay content composed only after first reveal; `LazyColumn` with stable keys.
- Drag moves pixels via `graphicsLayer` only.
- Target: no dropped frames during reveal on a mid-tier device; verify with a simple Macrobenchmark or manual profiler pass.

---

## 16. Acceptance checklist

- [ ] Drag/slider reveals the shadow homescreen with spring physics; handle + edge tab + back-press all work.
- [ ] Shadow task container content == user task container content (same order, same cards), completion marks per Cases 1 & 2.
- [ ] A task left incomplete 6h shows shadow-completed in the container **and** moves shadow consistency with no user action and no user-side mutation.
- [ ] User radar and user consistency unchanged by all of the above.
- [ ] Mercy and 99 ceiling verified in tests.
- [ ] Colors match tokens (screenshot diff vs mock).

---

## 17. Open questions → defaults (proceed with default unless the human overrides)

| Question | Default |
|----------|---------|
| Focus axis label: SKILL or FOCUS? | Display "Focus" |
| Shadow "+" tap behavior | The shadow "+" button should be non-functional. |
| Slide direction | From right; swiping left reveals the shadow homescreen. |
| Quiet hours for the 6h rule | None; shadow never sleeps. |
| Keep `CONSISTENCY_BOOST_PER_CLAIM`? | **No** — removed (Step 3). |

> Delete Anything that isn't needed or irrelavent.