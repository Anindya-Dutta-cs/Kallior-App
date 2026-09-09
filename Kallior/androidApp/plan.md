You are working on my existing Android application.

I want you to redesign and implement the current home/dashboard screen from TOP TO BOTTOM so that it closely matches the attached reference image.

IMPORTANT:
- The attached image is the visual design reference.
- Do NOT simply create a static imitation or screenshot.
- Rebuild the actual Android UI using the project's existing architecture, components, navigation, state management, backend integration, and data models.
- Inspect the existing codebase before making changes.
- Do not replace working functionality just to achieve the visual design.
- Do not create fake/mock data where real app data already exists.
- Maintain existing functionality unless explicitly instructed otherwise.
- Follow the existing project's coding conventions.
- Make the implementation production-quality, responsive, maintainable, and animation-friendly.
- The final result should feel like the same app evolved into a much more polished product, not like a completely different application.

==================================================
1. FIRST: INSPECT THE PROJECT
==================================================

Before writing code:

1. Determine:
   - Android framework being used.
   - Jetpack Compose vs XML Views.
   - Navigation architecture.
   - State management approach.
   - Database/backend architecture.
   - Existing task models.
   - Existing reminder models.
   - Existing statistics/progress/radar-chart implementation.
   - Existing theme/colors/typography.
   - Existing reusable UI components.
   - Existing animation utilities.
   - Existing Supabase integration, if present.

2. Find the current implementation of:
   - Task cards.
   - Reminder cards.
   - Radar chart.
   - Add-task functionality.
   - Add-reminder functionality.
   - Task completion.
   - Task deletion.
   - User statistics.
   - Navigation/bottom navigation.

3. Reuse existing components and services wherever practical.

4. Do NOT create duplicate repositories, duplicate data models, duplicate database logic, or parallel implementations just because the UI is being redesigned.

5. Before editing anything, briefly map out:
   - Which files control the screen.
   - Which files control the data.
   - Which files control navigation.
   - Which files control theme/design.
   - Which files need to change.
   - Which existing functionality must remain untouched.

Then implement the redesign.

==================================================
2. DESIGN GOAL
==================================================

The goal is a:

- sleek
- modern
- elegant
- premium
- minimal
- dark
- slightly futuristic
- motivational

dashboard.

The screen should feel alive without becoming visually noisy.

The visual language should communicate:

"Your potential is visible, measurable, and constantly changing."

The interface should feel deliberate.

Avoid:
- excessive cards
- excessive borders
- excessive shadows
- repetitive rectangles
- too many competing colors
- unnecessary labels
- overly large buttons
- clutter
- generic Material Design appearance
- default Android component styling

The reference image should be treated as the visual direction.

==================================================
3. OVERALL SCREEN STRUCTURE
==================================================

The screen should be structured approximately like this:

TOP
|
| Header
|
| Radar / personal-performance visualization
|
| Subtle transition / visual separator
|
| Today's progress / action area
|
| Tasks
|
| Reminder
|
| Badges
|
| Bottom navigation
|
BOTTOM

However, the screen must remain vertically scrollable where appropriate.

The radar section should receive significantly more visual importance than in the current implementation.

Tasks and reminders should feel like polished content rather than generic rectangular containers.

==================================================
4. BACKGROUND
==================================================

Use a deep near-black background.

Do NOT use pure #000000 everywhere.

Use subtle tonal differences to create depth.

Suggested visual direction:

Main background:
#050505
or a very similar near-black.

Secondary surfaces:
slightly brighter dark charcoal tones.

Use extremely subtle radial/linear gradients where appropriate.

The screen should remain primarily black/dark.

The existing orange accent must remain the primary accent color.

Use orange for:
- active states
- icons
- progress
- important controls
- highlights
- interaction feedback
- radar chart
- selected navigation state

Avoid introducing random colors unless semantically necessary.

For example, reminder categories may use restrained secondary colors, but orange should remain the primary identity color.

==================================================
5. HEADER
==================================================

At the top:

Display the user's identity/title prominently.

Use a typography hierarchy similar to:

Small:
"Good morning,"

Large:
"You got this ✦"

The actual greeting should adapt dynamically where appropriate:
- Good morning
- Good afternoon
- Good evening

Do not hard-code the time of day.

The user's name can be incorporated if the existing application has it, but do not make the header excessively large.

Typography should feel elegant and modern.

Use strong contrast.

The header should have generous horizontal padding.

Add a small profile/account action on the right if the current application already has a profile/settings destination.

The profile icon should feel integrated into the visual system rather than looking like a default Material icon button.

==================================================
6. RADAR CHART — HERO ELEMENT
==================================================

THIS IS ONE OF THE MOST IMPORTANT CHANGES.

The radar chart should become a major visual focal point of the screen.

The current radar chart feels too small and passive.

Make it substantially larger.

The chart should visually dominate the upper-middle section.

The radar should represent the existing 5-axis user metrics.

Do NOT replace the existing metric calculations.

Use the real values already produced by the application.

==================================================
RADAR DESIGN
==================================================

Create:

- 5-axis radar chart
- multiple concentric rings
- subtle grid lines
- radial axis lines
- labels/icons around the outside
- orange data polygon
- subtle orange glow
- transparent orange fill inside the polygon
- clean, thin geometry

The chart itself should NOT look like a standard spreadsheet/chart component.

It should feel custom-designed for this app.

Suggested hierarchy:

Outer grid:
very low opacity

Inner grid:
slightly stronger

Data polygon:
high visibility

Data fill:
low-opacity orange

Data points:
small glowing circles with slight ripples

The orange data shape should be the visual focus.

==================================================
RADAR ANIMATION
==================================================

The radar should animate when the screen appears.

Animation sequence:

1. Radar grid fades in.
2. Axis lines subtly appear.
3. Data polygon grows from the center outward.
4. Data points appear.
5. Orange glow becomes slightly stronger.
6. Labels/icons fade in.

Use smooth easing.

Do NOT use a cheap "spinning chart" animation.

The chart should feel like it is being constructed.

Suggested duration:
approximately 600–1000ms total.

The animation should be subtle enough that users do not find it annoying.

==================================================
LIVE RADAR UPDATES
==================================================

When the user's statistics change:

DO NOT instantly snap the radar polygon to the new values.

Animate from:

previous values
→
new values

Interpolate each axis smoothly.

This should make the radar feel alive.

Example:

Focus:
72 → 78

Instead of changing instantly, animate:

72
73
74
75
76
77
78

while simultaneously morphing the polygon.

The chart should react naturally to changes in user performance.

==================================================
RADAR INTERACTION
==================================================

Allow tapping an axis/metric where practical.

When the user taps a metric:

- highlight that axis
- slightly increase the icon/label emphasis
- subtly highlight the corresponding radar point
- show the metric name
- show its current value
- optionally show a short contextual description if existing product logic supports it

Do not open an enormous dialog.

A small elegant tooltip/overlay is preferred.

For example:

FOCUS
78%

The interaction should disappear naturally when the user taps elsewhere.

==================================================
7. RADAR METRIC LABELS
==================================================

Each radar axis should have:

- an icon
- metric name
- value

Example:

Focus
78%

Discipline
81%

Productivity
65%

Energy
72%

Mood
68%

Use the ACTUAL application's five existing metrics rather than blindly replacing them.

Icons should be:

- simple
- thin
- orange
- visually consistent
- recognizable

Do not use giant icons.

The labels should sit outside the radar chart without overlapping it.

Make sure the layout works across different Android screen sizes.

Do NOT hard-code pixel coordinates that only work on one device.

==================================================
8. RADAR ↔ REST OF SCREEN TRANSITION
==================================================

Do not end the radar section with a harsh rectangular boundary.

Create a subtle visual transition.

A gentle dark/orange gradient or organic curved shape can transition from the radar area into the content section.

The transition should feel intentional and premium.

Avoid decorative effects that make the UI look like a gaming HUD.

The overall aesthetic is closer to:

Apple-quality minimalism
+
premium productivity app
+
subtle futuristic visual identity.

==================================================
9. TODAY'S PROGRESS / ACTION AREA
==================================================

Introduce a cleaner progress/action area below the radar.

This should replace the repetitive empty-card feeling.

Use one visually important container.

It should communicate something like:

Today's progress: which should Include the average of all 5 axes.

summation of values from all 5 axis / 5

with a horizontal progress bar.

The progress bar should animate when it changes.

The plus button should be integrated into the container.

The button should not look like a floating Material button placed randomly on top of the interface.

It should feel intentionally designed into the component.

==================================================
10. PLUS BUTTON
==================================================

The "+" button is important.

Design it as:

- circular
- orange
- slightly glowing
- premium
- tactile

Use a subtle outer glow.

When idle:

very subtle breathing/pulse animation.

DO NOT constantly pulse aggressively.

When pressed:

- slightly scale down
- brighten
- release with a soft spring animation

This should give the button physicality.

==================================================
11. ADD MENU INTERACTION
==================================================

When the user taps "+":

Do NOT immediately throw the user into a generic default dialog.

Instead, introduce an elegant action selection.

For example:

+ Add

Task
Reminder

This can appear as:

- bottom sheet
- floating expansion
- compact action menu

Use whichever pattern fits the existing app architecture best.

The appearance should animate from the "+" button.

Recommended animation:

1. Button subtly expands.
2. Two action choices fade/slide upward.
3. Background becomes slightly dimmer.
4. User selects Task or Reminder.

The transition should feel connected to the button.

==================================================
12. TASK SECTION
==================================================

The task section should be substantially more elegant than the current design.

Instead of:

Tasks
+
large empty rectangle

Use:

Tasks                         See all >

Then a vertical list of task items.

Each task item should have:

- completion control
- task title
- optional time
- optional metadata
- subtle container/surface
- clean spacing
- strong hierarchy

Avoid excessive borders.

The cards should look almost like soft floating surfaces.

==================================================
13. TASK CARD DESIGN
==================================================

Example visual hierarchy:

[ ○ ]  Morning workout
       08:00 AM                                      >

The completion control should be custom styled.

For incomplete:

thin circular outline.

For complete:

orange filled circle
+
checkmark.

Task titles should be clearly readable.

Secondary information should be smaller and lower contrast.

The right-side chevron should be subtle.

Do not visually overpower the title.

==================================================
14. TASK COMPLETION ANIMATION
==================================================

When a task is completed:

1. Circle animates from outline → orange.
2. Checkmark draws/appears smoothly.
3. A subtle highlight travels across the row.
4. The task text transitions to the completed state.
5. If the existing product logic changes the radar statistics, allow the radar to animate toward its new values.
6. Update progress statistics smoothly.

Do not immediately remove the item from the screen.

The user needs visual confirmation.

Use a short, polished transition.

==================================================
15. TASK DELETION
==================================================

Preserve the existing swipe-to-delete functionality.

The current app concept includes:

Swipe right on a task card
→ reveal deletion action.

Keep this interaction.

Improve it visually.

During swipe:

- task card follows finger naturally
- delete action appears progressively
- progress/circular icon can reduce opacity
- deletion affordance should become clearer as the swipe increases
- use a smooth threshold

Do NOT use abrupt jumps.

If the user releases before the deletion threshold:

→ card returns to original position.

If the user passes the threshold:

→ deletion completes with a polished exit animation.

Preserve the existing backend behavior and task counters.

==================================================
16. REMINDER SECTION
==================================================

Use the same visual language as Tasks but do not make it feel like a duplicated section.

Header:

Reminders                             See all >

Reminder items should have:

- reminder icon
- reminder title
- time/date

Use subtle visual differences to distinguish reminders from tasks.

Do not simply create another identical card design.

The user should immediately understand:

Tasks = actions I need to complete

Reminders = things I need to remember.

==================================================
17. REMINDER CREATION
==================================================

Tapping the reminder add action should use the same elegant interaction system as tasks.

The input flow should feel simple.

Prioritize:

- title
- time/date
- frequency of the reminder

Do not overload the first screen with unnecessary settings.

Secondary options can appear progressively.

Use animated transitions between states.

==================================================
18. EMPTY STATES
==================================================

This is especially important.

Do NOT show giant empty rectangular boxes saying:

"Task free..."
"Reminder-free mind!"
"Wow, such empty!"

The current empty-state presentation wastes space.

Instead use compact empty states.

For example:

Tasks

No tasks for today
Take a moment to plan your next move

[ + Add task ]

Make this feel intentional rather than like missing content.

For reminders:

No reminders yet
Keep something important on your radar

[ + Add reminder ]

For badges:

Use the space more meaningfully.

==================================================
19. BADGES
==================================================

Badges should not simply be:

Badges >
[empty card]

Create a small preview section.

For example:

Badges                              See all >

[ badge ][ badge ][ badge ]

If no badges exist:

show a compact motivational placeholder.

Example:

Your first badge is waiting.

Do not make empty states visually dominant.

==================================================
20. SCROLLING
==================================================

The entire screen should scroll naturally.

The radar should appear as a visual hero section at the top.

When scrolling:

- avoid excessive parallax
- optionally allow a tiny amount of motion on decorative glow elements
- preserve readability
- prevent jitter

The interface should feel smooth at 60/120 FPS where the device supports it.

==================================================
21. BOTTOM NAVIGATION
==================================================

If the existing app has bottom navigation:

redesign it to match the new visual language.

Use:

- dark surface
- subtle separation
- simple icons
- selected icon in orange
- selected label in orange or stronger contrast
- unselected items in subdued gray

Keep it minimal.

Do not turn it into a large floating pill unless that fits the existing application.

Navigation should remain persistent and predictable.

Follow Android accessibility and touch target guidelines.

The system navigation bar (android) should be the same color as the app navigation bar.

==================================================
22. TYPOGRAPHY
==================================================

Typography needs to create hierarchy.

Use the project's existing font if one already exists.

Otherwise use a high-quality system font or an appropriate bundled font already available in the project.

Hierarchy:

Hero heading:
large / bold

Section headings:
medium-large / semibold

Primary content:
regular / medium

Metadata:
small / low contrast

Avoid using too many weights.

The word "Yourself" / major branding text may retain the more elegant serif/italic personality seen in the original design if that is already part of the app's identity.

Use a modern sans-serif for functional UI.

This creates an intentional contrast:

Elegant personality
+
modern usability.

==================================================
23. SPACING
==================================================

Use a consistent spacing system.

Do not manually tune every element independently.

Use a baseline spacing scale such as:

4
8
12
16
20
24
32

Use larger spacing between major sections.

Use smaller spacing inside components.

The interface should breathe.

The current design feels like multiple isolated boxes.

The new design should feel like one continuous composition.

==================================================
24. CORNERS AND SHAPES
==================================================

Use consistent corner radii.

Suggested hierarchy:

Design principle:
The corner radii of elements should be 22.5% of the shorter side of the rectangle.

Large containers:
20–28dp

Normal cards:
16–20dp

Buttons:
circular or 14–18dp depending on component

Avoid mixing many unrelated corner radii.

The design should feel cohesive.

==================================================
25. BORDERS
==================================================

Do not outline every component with obvious gray borders.

Use:

- subtle tonal differences
- very low-opacity strokes
- shadows
- gradients
- separation through spacing

Only use borders where they genuinely improve clarity.

This directly addresses the current screen's repetitive visual pattern.

==================================================
26. ORANGE ACCENT
==================================================

Orange is the primary accent.

Use it carefully.

Orange should mean:

- action
- progress
- active
- important
- achievement
- feedback

Do not make the entire interface orange.

The power of the design should come from contrast:

black
+
dark charcoal
+
small amounts of glowing orange.

==================================================
27. MICRO-INTERACTIONS
==================================================

The application should feel alive.

Add subtle animations for:

- page appearance
- radar chart
- radar updates
- task completion
- task deletion
- reminder creation
- plus button
- section appearance
- progress bar
- navigation selection
- button presses

Animations must be:

- fast
- intentional
- subtle
- physically believable

Avoid:
- constant bouncing
- excessive scaling
- long transitions
- random decorative animations
- distracting particle effects.

The user should feel the interface reacting, not watching an animation demo.

==================================================
28. SCREEN ENTRY ANIMATION
==================================================

When the screen opens:

Do not animate everything independently.

Use a coordinated entrance.

Suggested sequence:

0–150ms:
header fades/slides in

100–500ms:
radar appears

300–650ms:
progress component appears

450–800ms:
task content appears

600–900ms:
reminder/badge content appears

Keep movement extremely subtle.

The radar is the primary animation.

==================================================
29. PERFORMANCE
==================================================

This is a real mobile application.

Do not sacrifice performance for visual effects.

Avoid:
- unnecessary recomposition
- excessive blur
- expensive continuous animations
- unnecessary canvas redraws
- huge bitmaps
- unbounded state updates

The radar chart should be implemented efficiently.

Animations should stop when not needed.

Respect lifecycle.

Do not leak coroutines/listeners.

==================================================
30. ACCESSIBILITY
==================================================

The redesign must still be accessible.

Ensure:

- adequate contrast
- minimum touch target sizes
- meaningful content descriptions
- screen-reader labels
- non-color-only state communication
- accessible task completion controls
- accessible swipe/delete behavior
- sensible text scaling

The visual design should not come at the cost of usability.

==================================================
31. RESPONSIVE DESIGN
==================================================

DO NOT design for one exact screenshot size.

The attached reference is only a visual target.

Support:

- small Android phones
- modern tall phones
- different aspect ratios
- different densities
- accessibility font scaling where possible

The radar must resize intelligently.

Labels must avoid collisions.

Nothing should be clipped.

Nothing should rely on absolute screen coordinates.

==================================================
32. DATA / BACKEND INTEGRITY
==================================================

IMPORTANT:

The visual redesign must NOT break existing backend behavior.

Preserve all existing task statistics and functionality.

In particular, existing task logic should continue to correctly handle:

- task creation
- task completion
- task deletion
- scheduled task count
- completed task count
- radar/statistics calculations

If Supabase is already integrated:

- reuse the existing Supabase client/repository layer
- do not instantiate a new client unnecessarily
- do not move database code into UI components
- do not duplicate backend logic

UI state should react to the existing data source.

==================================================
33. COMPONENT ARCHITECTURE
==================================================

Break the screen into sensible reusable components.

For example:

YourselfScreen
├── YourselfHeader
├── PerformanceRadar
│   ├── RadarGrid
│   ├── RadarPolygon
│   ├── RadarPoint
│   └── RadarMetricLabel
├── DailyProgressCard
├── SectionHeader
├── TaskList
│   └── TaskItem
├── ReminderList
│   └── ReminderItem
├── BadgePreview
└── BottomNavigation

Use names appropriate to the existing project's architecture.

Do not create one giant 1000+ line screen file.

Keep UI, business logic, and data concerns separated.

==================================================
34. STATE MANAGEMENT
==================================================

Do not introduce local state that duplicates existing application state.

The screen should react to real state.

Examples:

tasks update
→ task list updates
→ progress updates
→ statistics update
→ radar animates to new values.

A reminder is created
→ reminder list updates
→ UI animates naturally.

A task is deleted
→ backend updates
→ local state updates correctly
→ progress/statistics update.

==================================================
35. DESIGN DETAILS FROM THE REFERENCE
==================================================

Use these visual characteristics from the attached reference:

- near-black background
- warm orange accent
- subtle orange glow
- spacious layout
- elegant typography
- large radar visualization
- clear metric labels
- curved/organic transition below the radar
- premium dark surfaces
- minimal card borders
- rounded components
- polished circular plus button
- compact task rows
- compact reminder rows
- clear section headings
- restrained bottom navigation
- strong visual hierarchy
- minimal clutter

Do not blindly reproduce every pixel.

Instead reproduce the DESIGN LANGUAGE and interaction philosophy.

==================================================
36. WHAT SHOULD CHANGE FROM THE CURRENT SCREEN
==================================================

The current screen has several problems:

1. Too many visually similar rectangular cards.
2. Empty sections consume too much vertical space.
3. Radar chart feels too small.
4. Tasks/reminders feel disconnected from the rest of the screen.
5. Plus buttons feel attached rather than integrated.
6. There is little visual hierarchy.
7. There is not enough motion or feedback.
8. The page does not strongly communicate what deserves attention first.
9. The interface has too many repetitive patterns.
10. The screen does not yet feel like a premium finished product.

The redesigned version should directly solve these problems.

==================================================
37. IMPORTANT UX PRINCIPLE
==================================================

The user should understand the entire screen in roughly 2–3 seconds.

At a glance they should know:

1. How am I doing?
2. What is my current potential/performance?
3. What do I need to do?
4. What do I need to remember?
5. What can I do next?

The screen should never make the user hunt for important actions.

==================================================
38. IMPLEMENTATION PROCESS
==================================================

Work in this order:

PHASE 1 — INSPECT
- understand codebase
- locate screen
- locate data/state
- locate theme
- locate navigation

PHASE 2 — ARCHITECTURE
- determine reusable components
- determine where the redesign should live
- identify any existing technical limitations

PHASE 3 — BUILD VISUAL FOUNDATION
- background
- theme
- typography
- spacing
- shapes
- colors

PHASE 4 — BUILD RADAR
- grid
- polygon
- points
- labels
- animation
- interactions
- live interpolation

PHASE 5 — BUILD PROGRESS
- progress card
- animated progress
- integrated add button

PHASE 6 — BUILD TASKS
- section
- task items
- completion
- swipe-to-delete
- transitions

PHASE 7 — BUILD REMINDERS
- section
- items
- add flow

PHASE 8 — BUILD BADGES
- preview
- empty state

PHASE 9 — NAVIGATION
- bottom navigation
- selected/unselected states

PHASE 10 — POLISH
- micro-interactions
- animation timing
- spacing
- accessibility
- responsiveness
- performance

==================================================
39. VALIDATION
==================================================

After implementation:

1. Compile the project.
2. Fix all compilation errors.
3. Fix warnings that are directly related to your changes.
4. Run the application.
5. Inspect the redesigned screen.
6. Compare it against the attached reference.
7. Check multiple screen dimensions if possible.
8. Verify scrolling.
9. Verify task creation.
10. Verify task completion.
11. Verify task deletion.
12. Verify reminder creation.
13. Verify radar updates.
14. Verify statistics remain accurate.
15. Verify navigation.
16. Verify animations.
17. Verify accessibility.

Do not stop after writing the UI code.

The task is only complete when the screen actually works.

==================================================
40. FINAL QUALITY BAR
==================================================

The final result should feel like a polished production app from a professional product/design team.

Ask yourself:

- Does the radar feel like the hero?
- Does orange feel intentional rather than excessive?
- Does the page breathe?
- Are tasks easy to scan?
- Are empty states elegant?
- Is the add interaction satisfying?
- Do animations communicate state?
- Does completion feel rewarding?
- Does deletion feel natural?
- Does the screen feel cohesive?
- Does anything look like a default Android component?
- Are there unnecessary boxes?
- Are there unnecessary labels?
- Does the user immediately know where to look?
- Does this feel alive without being distracting?

If the answer to any of these is no, refine the implementation.

==================================================
41. IMPORTANT: DO NOT DO THESE THINGS
==================================================

Do NOT:

- replace the existing app architecture unnecessarily
- rewrite unrelated screens
- create fake backend logic
- hard-code task data
- remove existing task functionality
- remove existing reminder functionality
- remove statistics
- introduce unnecessary dependencies
- use huge amounts of blur
- use excessive gradients
- use excessive animations
- create a generic Material 3 dashboard
- create dozens of independent cards
- make the UI look like a crypto/AI/gaming dashboard
- use bright orange everywhere
- sacrifice accessibility
- sacrifice performance
- use absolute positioning for the entire screen
- create one enormous composable/view containing everything

==================================================
42. DELIVERABLE
==================================================

Implement the redesign directly in the existing project.

At the end, report:

1. Files changed.
2. Components created.
3. Components modified.
4. Animations added.
5. Any backend/data logic touched.
6. Any new dependencies added.
7. Any issues that could not be completed.
8. Build/test result.

Do not just tell me what you would implement.

Actually implement it.