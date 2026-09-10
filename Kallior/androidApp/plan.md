You are redesigning an existing Android application's "Zen Silo" screen.

The CURRENT screen should be treated as the starting point and visual reference for understanding the current functionality.

Your job is to redesign and IMPLEMENT this screen from top to bottom.

Do not merely describe a design.
Do not create a static mockup.
Do not replace the screen with hard-coded fake data.

Inspect the existing project first, understand how this screen currently works, and then implement the redesign directly in the existing application.

The result should feel like a polished production application with a strong visual identity, excellent UX, deliberate motion, and clear information hierarchy.

==================================================
1. CORE PRODUCT CONCEPT
==================================================

This screen represents "Zen Silo"/"Focus Fortress", a digital protection/distraction-control system.

The screen communicates:

- protection is active
- distracting apps are restricted
- distracting websites are restricted
- screen-time impact
- time/value lost or potentially recovered
- which apps are responsible for distraction
- the user's current protection state

The lotus is the primary visual identity of Zen Silo.

The lotus should NOT remain a static image.

It should become an interactive animated representation of the protection system.

The redesign must preserve this concept.

==================================================
2. FIRST: INSPECT THE EXISTING PROJECT
==================================================

Before modifying anything, inspect the entire relevant implementation.

Determine:

- Android framework
- Jetpack Compose or XML Views
- navigation architecture
- state management
- database/backend
- repositories
- services
- screen-time tracking implementation
- app-blocking implementation
- website-blocking implementation
- protection-state implementation
- current Zen Silo screen
- existing theme
- existing icons
- existing animation utilities
- existing data models
- existing navigation destinations

Locate the actual implementation of:

- protection status
- time sink
- total screen time
- limited apps
- limited websites
- blocked app statistics
- potential earnings/value calculation
- app usage breakdown
- website restriction state

Do not duplicate existing business logic.

Do not create a second source of truth.

Do not hard-code values that already exist in the application.

Reuse the existing architecture wherever possible.

Before editing, identify the files responsible for:

1. Zen Silo UI
2. Protection state
3. Screen-time data
4. Blocked-app data
5. Blocked-website data
6. Value/earnings calculations
7. Navigation
8. Theme/design system

==================================================
3. HIGH-LEVEL DESIGN GOAL
==================================================

Redesign the screen so it feels:

- sleek
- modern
- calm
- premium
- intentional
- minimal
- sophisticated
- slightly futuristic
- alive

The visual language should feel appropriate for a digital-wellbeing/productivity application.

It should NOT feel like:

- a gaming dashboard
- a cyberpunk interface
- a generic Material dashboard
- a finance app
- a settings screen
- a collection of unrelated cards

The emotional tone should be:

"Your digital environment is under control."

The interface should communicate calm control rather than stress.

==================================================
4. PRIMARY UX PRINCIPLE
==================================================

The user should understand the screen in approximately 2–3 seconds.

They should immediately understand:

1. Is protection active?
2. What is protection doing?
3. How much screen time/distraction is being prevented?
4. What apps/websites are restricted?
5. What is the impact?

The order of visual importance should roughly be:

PROTECTION STATUS
↓
LIVE LOTUS VISUALIZATION
↓
TODAY'S IMPACT
↓
RESTRICTIONS
↓
DETAILED USAGE

Do not allow secondary statistics to compete visually with the protection state.

==================================================
5. SCREEN STRUCTURE
==================================================

Redesign the screen approximately in this order:

HEADER

Zen Silo
optional settings/action button

PROTECTION HERO

Animated lotus
Protection status
Active/inactive state
Optional supporting information

IMPACT SUMMARY

Time saved/recovered
Total screen time
Potential value/impact

RESTRICTIONS

Limited apps
Limited websites

DISTRACTION DETAILS

Apps that account for the most time
Detailed usage/value breakdown

BOTTOM NAVIGATION

The exact layout may change based on the existing application's navigation system.

==================================================
6. HEADER
==================================================

Keep:

"Zen Silo"

as the page identity.

However, improve its typography and spacing.

The title should feel premium and intentional.

Use a strong typographic hierarchy.

Consider:

Zen Silo

with a subtle secondary descriptor only if the existing product requires one.

Do not clutter the header.

If the existing screen has a settings/control button, integrate it into the header rather than placing a floating generic icon somewhere arbitrary.

==================================================
7. PROTECTION HERO — MOST IMPORTANT COMPONENT
==================================================

The lotus is the visual centerpiece.

The current application has a static lotus image with ripples around it.

Replace the static presentation with an animated status visualization.

The lotus should visually communicate the current protection state.

==================================================
8. LOTUS VISUAL DESIGN
==================================================

Preserve the recognizable lotus identity from the existing design.

Do not completely replace the concept.

However, make the composition more sophisticated.

The lotus can contain:

- central lotus artwork
- soft circular halo
- multiple concentric rings
- subtle breathing glow
- low-opacity particles/light
- status-dependent visual changes

Avoid excessive decoration.

It should feel like a living system rather than a GIF.

==================================================
9. ACTIVE STATE
==================================================

When protection is active:

The lotus should have a subtle continuous breathing animation.

Recommended behavior:

- scale very slightly
- glow intensity changes subtly
- outer ring expands slowly
- ring fades as it expands
- another ring follows
- center remains visually stable

The animation must be slow and calming.

Do NOT create a large pulsing effect.

The user should notice movement without feeling distracted.

==================================================
10. PROTECTION RIPPLES
==================================================

The current design contains static ripples.

Convert them into actual animated ripples.

Possible sequence:

A ring originates from the lotus.

It slowly expands.

Opacity decreases.

It disappears.

A new ring begins.

Use staggered timing.

However:

Do not animate too many rings simultaneously.

Keep the motion elegant and subtle.

The animation should communicate:

"Protection is continuously active."

==================================================
11. PROTECTION STATE TEXT
==================================================

Instead of placing:

"Protection is active"

as a passive sentence beneath the image, turn it into a clear status component.

Example conceptual structure:

● PROTECTED

Protection is active

or:

Protected
Blocking distractions

Use typography and visual treatment to establish a clear status hierarchy.

The status should be instantly understandable.

Orange can be used here because it carries semantic meaning:

ACTIVE / PROTECTED.

Do not use orange for every element on the page.

==================================================
12. INACTIVE STATE
==================================================

The animation system must support an inactive state.

When protection is disabled:

- lotus becomes subdued
- glow becomes minimal
- ripples stop
- active orange becomes muted
- status changes clearly
- UI provides an obvious action to enable protection

Example:

Protection paused

[ Enable Protection ]

The inactive state must be visually distinct from the active state.

Do not rely exclusively on color.

==================================================
13. EVENT-DRIVEN LOTUS ANIMATION
==================================================

Make the lotus respond to actual protection events.

When an app is blocked:

Trigger a brief visual reaction.

Example:

- pulse
- short brighter glow
- one expanding ripple
- subtle scale change

When a website is blocked:

Use a similar but slightly different event reaction.

When protection begins:

Use a stronger activation animation.

When protection stops:

Use a graceful fade-out/deactivation animation.

These animations should communicate actual state changes.

Do not animate randomly.

==================================================
14. IMPACT METRICS
==================================================

The current screen has:

Time Sink
Total Screen Time

Improve the presentation dramatically.

These numbers should become visually readable metrics.

Instead of tiny labels and dashes, use:

TIME RECOVERED
1h 42m

SCREEN TIME
3h 18m

Use the application's actual data.

Do not invent statistics.

Use a hierarchy where the numbers are significantly more prominent than their labels.

==================================================
15. NUMBER ANIMATIONS
==================================================

When metrics update:

Do not instantly replace the values.

Animate numeric transitions.

Example:

1h 35m
→
1h 42m

The numbers should smoothly interpolate where appropriate.

Keep the animation short.

The user should perceive it as responsive feedback.

==================================================
16. DAY-TO-DAY CONTEXT
==================================================

Where existing data supports it, display a subtle comparison.

Examples:

↓ 18% from yesterday

or:

12m less than yesterday

Only show comparisons when the underlying data genuinely supports them.

Never create fake trends.

This information should remain secondary.

==================================================
17. RESTRICTION SECTION
==================================================

Create a section for:

Limited Apps
Limited Websites

However, do NOT make them two generic identical cards.

They should have strong semantic identity.

For example:

LIMITED APPS
12 blocked

LIMITED WEBSITES
8 blocked

Use different icons that immediately communicate their purpose.

Examples:

Apps:
app/grid/app-lock style icon

Websites:
globe/browser style icon

The icons should not all have identical visual weight.

==================================================
18. ICON COLOR SYSTEM
==================================================

This is a major problem in the existing screen.

Currently many icons and buttons use the same orange.

Do not do that.

Create a restrained semantic hierarchy.

Suggested system:

Orange:
primary action
active protection
important positive status

Neutral/light:
secondary information
normal navigation
supporting controls

Muted gray:
inactive states
metadata
secondary labels

Optional restrained secondary accent:
categorization only, if necessary

Do NOT use multiple bright accent colors everywhere.

The interface should remain visually calm.

==================================================
19. BUTTON DESIGN
==================================================

The current orange circular buttons feel repetitive.

Replace them with more intentional controls.

Primary controls should use the application's main accent.

Secondary controls should be neutral or tonal.

Examples:

Primary:
Enable protection

Secondary:
Manage apps

Secondary:
Manage websites

Do not make every card contain a bright orange circular button.

==================================================
20. "APPS THAT WASTE YOUR TIME"
==================================================

This is useful information but currently looks like another generic card.

Redesign it as a meaningful summary.

Possible structure:

Apps taking your time

Instagram                  42m
YouTube                    31m
TikTok                     24m

or:

Top distractions

Instagram       42m
YouTube         31m
TikTok          24m

Only use real data from the application.

The user should be able to immediately identify where their attention is going.

==================================================
21. TOP DISTRACTION VISUALIZATION
==================================================

Consider using:

- short horizontal usage bars
- subtle progress indicators
- compact rows
- app icons
- time labels

Avoid turning this into a complicated chart.

A simple ranked list should be enough.

Example:

Instagram
██████████
42m

YouTube
███████
31m

TikTok
██████
24m

The bars should use restrained visual treatment.

Orange should indicate important/highest-impact data, not every bar.

==================================================
22. APP ICONS
==================================================

Use real app icons if the application already obtains them.

Do not use generic placeholder symbols when real app information is available.

App icons should be small and consistent.

Avoid making app icons visually louder than the usage statistics.

==================================================
23. VALUE / EARNINGS SECTION
==================================================

The current:

"You could've earned"

section feels visually disconnected.

Redesign it into an impact section.

Possible concept:

TIME / VALUE AT RISK

$12.40

Estimated value associated with your distraction time.

Then optionally show contributing apps below.

Example:

Instagram       42m      $4.20
YouTube         31m      $3.10
TikTok          24m      $2.40

Use the application's actual calculation.

Do not imply guaranteed earnings if the metric is only an estimate.

The wording must accurately represent what the existing calculation means.

==================================================
24. EMPTY STATES
==================================================

Never show large empty cards filled with placeholder text.

If no distracting apps have been recorded:

You are clear.

No significant distractions detected.

or an equivalent concise message.

If no limited apps exist:

No apps are currently restricted.

[ Manage apps ]

If no limited websites exist:

No websites are currently restricted.

[ Manage websites ]

The empty state should feel intentional and calm.

==================================================
25. CARD SYSTEM
==================================================

Reduce the number of visually identical cards.

Use three visual levels:

LEVEL 1 — HERO
Protection/lotus

LEVEL 2 — IMPORTANT
Metrics/restrictions

LEVEL 3 — DETAIL
Usage/value breakdown

Different hierarchy should be communicated through:

- spacing
- typography
- tonal surfaces
- size
- emphasis

Not merely by adding borders.

==================================================
26. BORDER USAGE
==================================================

Avoid outlining every component.

The current UI relies heavily on outlined rounded rectangles.

Replace many borders with:

- tonal contrast
- subtle surfaces
- shadows
- gradients
- spacing
- small dividers

Borders should be used selectively.

==================================================
27. SURFACE DESIGN
==================================================

Use subtle dark surfaces against the near-black background.

Do not make every surface identical.

Example hierarchy:

Background:
near-black

Primary surface:
slightly lighter charcoal

Secondary surface:
slightly different charcoal

Primary accent:
orange

This creates depth without requiring excessive gradients.

==================================================
28. TYPOGRAPHY
==================================================

Use typography to communicate hierarchy.

Suggested hierarchy:

Page title:
strong

Protection status:
medium/strong

Hero status:
large

Metrics:
large numeric values

Section headings:
medium

Metadata:
small

Supporting descriptions:
small/low contrast

Do not use excessive font weights.

Do not make every label uppercase.

Uppercase can be used selectively for tiny category labels.

==================================================
29. SPACING
==================================================

Introduce much more intentional spacing.

Major sections should be clearly separated.

Avoid the sensation of:

card
card
card
card

Instead the screen should feel like a continuous visual narrative.

Use a consistent spacing system.

Suggested spacing scale:

4dp
8dp
12dp
16dp
20dp
24dp
32dp

Use larger spacing between sections.

Use smaller spacing inside components.

==================================================
30. LOTUS ANIMATION PERFORMANCE
==================================================

The lotus animation must be efficient.

Do not continuously redraw expensive elements unnecessarily.

Prefer efficient Android animation APIs and the project's current framework.

Avoid:

- excessive blur
- giant bitmap effects
- unnecessary particle systems
- expensive continuous canvas operations

The animation should remain smooth on mid-range Android devices.

==================================================
31. SCROLL BEHAVIOR
==================================================

The full content area should scroll naturally.

The top hero should remain visually important.

Do not make the entire screen feel like a giant scrolling list of cards.

Consider subtle motion while scrolling, but keep it restrained.

Do not use dramatic parallax.

==================================================
32. INTERACTION WITH PROTECTION STATUS
==================================================

The protection hero should be interactive.

Possible behavior:

Tap protection hero
→ open protection controls/details.

Do not hide important controls inside animation.

The active state must still be understandable for users who do not interact.

==================================================
33. LIMITED APPS INTERACTION
==================================================

Tapping:

Limited Apps

should open the existing app restriction management screen.

The transition should feel intentional.

The card should provide tactile feedback.

When pressed:

- slight scale or tonal change
- subtle ripple
- immediate navigation

Do not create a new management screen unless the project does not already have one.

==================================================
34. LIMITED WEBSITES INTERACTION
==================================================

Tapping:

Limited Websites

should open the existing website restriction management screen.

Use the same interaction principles as Limited Apps.

However, visually distinguish the concept through iconography/content rather than making the entire component a different bright color.

==================================================
35. APP BLOCK EVENT
==================================================

When an application is blocked:

The Zen Silo screen should be capable of reflecting the event.

For example:

- blocked counter increments
- time/value metrics update
- lotus briefly reacts
- usage list updates
- values animate

Do not reload the entire screen abruptly.

Updates should feel continuous.

==================================================
36. PROTECTION TOGGLE
==================================================

If the existing application allows protection to be enabled/disabled:

The interaction should feel important.

When enabling:

1. User initiates action.
2. UI confirms intent if required.
3. Lotus activation animation begins.
4. Protection state changes.
5. Supporting UI updates.
6. Control returns to stable active state.

When disabling:

1. Lotus animation winds down.
2. State changes.
3. UI clearly communicates inactive mode.

Preserve all existing backend/service behavior.

==================================================
37. MICRO-INTERACTIONS
==================================================

Add purposeful animation to:

- protection activation
- protection deactivation
- lotus idle state
- blocking events
- number changes
- restriction card presses
- list updates
- navigation
- buttons
- expandable detail sections

Animations should feel:

- fast
- elegant
- smooth
- intentional

Avoid:

- bouncing everything
- constant pulsing
- flashy gradients
- excessive spring animations
- animation for decoration alone

Every animation should communicate something.

==================================================
38. MOTION LANGUAGE
==================================================

Define a coherent motion system.

Examples:

Small interaction:
100–180ms

Normal component transition:
200–300ms

Hero animation:
500–1000ms

Ambient lotus animation:
slow and continuous

Use appropriate easing.

Avoid arbitrary animation durations.

Animations should feel like they belong to the same application.

==================================================
39. ACCESSIBILITY
==================================================

Ensure:

- sufficient contrast
- accessible text sizes
- correct content descriptions
- meaningful screen-reader labels
- large enough touch targets
- no state communicated only through color
- reduced-motion compatibility where practical

If the system requests reduced motion:

- minimize or disable continuous lotus animation
- preserve state communication
- preserve functionality

==================================================
40. RESPONSIVE DESIGN
==================================================

The screenshot is only a visual reference.

Do NOT hard-code exact screen coordinates.

Support:

- small Android phones
- tall phones
- different aspect ratios
- different densities
- text scaling

The lotus should resize intelligently.

Text should not overlap.

Metrics should wrap gracefully.

Cards should not become distorted on unusual screen sizes.

==================================================
41. BUSINESS LOGIC / DATA INTEGRITY
==================================================

This is extremely important.

Do not break:

- screen-time tracking
- app blocking
- website blocking
- restriction state
- protection state
- usage calculations
- value calculations
- navigation
- backend synchronization

If Supabase or another backend is already in the application:

reuse the existing client/repositories/services.

Do not instantiate duplicate clients.

Do not move backend calls directly into UI components.

Keep data and presentation separated.

==================================================
42. ARCHITECTURE
==================================================

Keep the screen modular.

Use appropriate reusable components.

Conceptually:

ZenSiloScreen
├── ZenSiloHeader
├── ProtectionHero
│   ├── LotusVisualization
│   ├── ProtectionStatus
│   └── ProtectionControls
├── ImpactSummary
│   ├── TimeRecoveredMetric
│   └── ScreenTimeMetric
├── RestrictionsSection
│   ├── LimitedAppsCard
│   └── LimitedWebsitesCard
├── DistractionSection
│   ├── TopDistractionHeader
│   └── DistractionItem
├── ValueImpactSection
│   └── ValueItem
└── BottomNavigation

Use names appropriate to the existing project.

Do not create one massive screen file.

==================================================
43. DO NOT REBUILD UNRELATED SCREENS
==================================================

Only modify components necessary to support this redesign.

Do not rewrite:

- authentication
- unrelated screens
- unrelated repositories
- unrelated navigation
- unrelated backend systems

unless the redesign genuinely requires a small shared component change.

==================================================
44. DESIGN CONSISTENCY WITH THE REST OF THE APP
==================================================

The screen should feel like it belongs to the same application as the redesigned "Yourself" screen.

Maintain the existing app identity:

- dark aesthetic
- elegant typography
- orange as the primary accent
- minimal UI
- subtle modern motion

However, Zen Silo should have its own visual personality.

It should feel calmer and more protective than the "Yourself" screen.

Think:

Yourself = personal performance

Zen Silo = digital protection / calm / control

==================================================
45. VISUAL HIERARCHY TEST
==================================================

When looking at the finished screen for one second, the eye should go approximately:

1. Lotus / protection status
2. Main metric
3. Restrictions
4. Supporting information
5. Detailed usage

If all elements appear equally important, redesign the hierarchy.

==================================================
46. REMOVE VISUAL REPETITION
==================================================

The current screen has too many repeated patterns:

- same orange icon treatment
- same orange circular buttons
- same outlined rectangular cards
- similar text arrangement

Actively eliminate this repetition.

Use variation through:

- scale
- spacing
- typography
- icon treatment
- surface treatment
- content density

But maintain enough consistency that the UI still feels like one system.

==================================================
47. VISUAL RESTRAINT
==================================================

Do not add effects simply because they are possible.

Avoid:

- excessive glow
- excessive blur
- giant shadows
- particle explosions
- constant movement
- multiple bright accent colors
- huge gradients
- overly complicated charts

The interface should feel expensive because it is restrained.

==================================================
48. IMPLEMENTATION PROCESS
==================================================

Follow this exact workflow:

PHASE 1
Inspect existing implementation.

PHASE 2
Identify current architecture and dependencies.

PHASE 3
Create/refine design tokens.

PHASE 4
Rebuild the header.

PHASE 5
Rebuild the protection hero.

PHASE 6
Implement animated lotus.

PHASE 7
Implement protection-state interactions.

PHASE 8
Rebuild impact metrics.

PHASE 9
Rebuild limited app/website controls.

PHASE 10
Rebuild top distraction section.

PHASE 11
Rebuild value/impact section.

PHASE 12
Add transitions and micro-interactions.

PHASE 13
Validate responsiveness/accessibility.

PHASE 14
Build and test the entire application.

==================================================
49. TESTING REQUIREMENTS
==================================================

After implementation:

1. Compile the project.
2. Resolve compilation errors.
3. Run the application.
4. Open Zen Silo.
5. Verify protection active state.
6. Verify protection inactive state.
7. Verify lotus animation.
8. Verify blocking events.
9. Verify screen-time numbers.
10. Verify restriction counts.
11. Verify app navigation.
12. Verify website navigation.
13. Verify detailed usage.
14. Verify value calculations.
15. Verify scrolling.
16. Verify text scaling.
17. Verify touch targets.
18. Verify performance.

Do not stop after compiling.

Actually inspect the rendered result.

==================================================
50. REFERENCE IMAGE
==================================================

Treat the attached screenshot as a reference for:

- existing layout
- existing lotus identity
- existing content
- existing information
- current brand feel

Do NOT copy the old visual implementation literally.

Improve it.

The final screen should look like:

"Zen Silo 2.0"

rather than simply a recolored version of the old UI.

==================================================
51. FINAL QUALITY CHECK
==================================================

Before declaring the work complete, ask:

Does the lotus feel alive?

Is protection the first thing I understand?

Can I immediately tell whether protection is active?

Are the statistics readable without effort?

Are apps and websites visually distinguishable?

Are orange elements reserved for meaningful states/actions?

Have the repeated outlined cards been eliminated?

Does the screen feel calm rather than cluttered?

Do animations communicate actual events?

Does the screen feel premium?

Does anything look like a default Android component?

Does the interface still work if animations are disabled?

Does everything use real application data?

Does the screen remain usable on smaller devices?

Is the result visually consistent with the rest of the application?

If the answer to any of these is no, refine the implementation.

==================================================
52. FINAL REPORT
==================================================

After implementation, provide:

1. Files changed
2. New components created
3. Existing components modified
4. Animation systems added
5. Interaction changes
6. Data/backend logic touched
7. Dependencies added, if any
8. Testing performed
9. Any known issues
10. Any design decisions that were necessary because of limitations in the existing codebase

Most importantly:

IMPLEMENT THE SCREEN.

Do not merely give me instructions.
Do not give me pseudo-code.
Do not stop at a design proposal.

Make the changes in the actual project.