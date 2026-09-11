You are redesigning and expanding an existing Android application's "AriaAlarm" screen.

Your task is to redesign the screen from TOP TO BOTTOM and implement the redesign directly in the existing Android project.

This is NOT a request for a static mockup.

You must:

1. Inspect the existing codebase.
2. Understand the current AriaAlarm architecture and functionality.
3. Preserve working functionality.
4. Redesign the UI.
5. Add support for multiple alarms.
6. Add per-alarm custom songs.
7. Add per-alarm names.
8. Add enable/disable state per alarm.
9. Add the necessary persistence/data logic.
10. Add polished interactions and animations.
11. Build and test the result.

The final screen should feel like a real, polished product rather than a settings screen.

==================================================
1. PRODUCT CONCEPT
==================================================

AriaAlarm is the user's personalized morning alarm system.

The core experience should communicate:

"My morning starts with something I chose."

The user should be able to:

- upload/import songs
- maintain a library of songs
- create multiple alarms
- give every alarm a custom name
- assign a different song to each alarm
- set the time
- enable/disable each alarm
- edit existing alarms
- delete alarms
- preview a song
- understand which alarm will ring next

The UI should make the experience feel personal and premium.

The application already has a strong dark visual identity.

Continue that identity.

==================================================
2. DESIGN DIRECTION
==================================================

Redesign AriaAlarm so it feels:

- sleek
- modern
- elegant
- premium
- minimal
- atmospheric
- calm
- slightly futuristic
- personal
- alive

It should NOT look like:

- a generic Android alarm clock
- a settings page
- a collection of Material cards
- an audio player
- a generic productivity dashboard

The design should feel consistent with the rest of my application.

The visual identity should continue using:

- near-black backgrounds
- warm orange as the primary accent
- elegant typography
- subtle gradients
- restrained borders
- soft shadows
- rounded surfaces
- intentional spacing
- minimal iconography

Orange should NOT be used for every icon and control.

Use it primarily for:

- active states
- primary actions
- important status
- selected elements
- confirmation
- key visual emphasis

Use neutral tones for secondary information and controls.

==================================================
3. CURRENT PROBLEMS TO SOLVE
==================================================

The current screen has these problems:

1. Too much unused vertical space.
2. The screen feels visually empty.
3. There is only one large alarm container.
4. The interface feels like a settings screen.
5. The Time and Enabled controls look like generic settings rows.
6. The song is separated from the alarm concept.
7. The plus button does not communicate "create a new alarm" strongly enough.
8. The alarm lacks a visual identity.
9. There is no clear hierarchy around the NEXT alarm.
10. The interface does not feel alive.
11. The same orange color is repeatedly applied to icons, switches, and actions.
12. There is no elegant way to manage multiple alarms.
13. The user cannot associate a name with an alarm.
14. The user cannot clearly understand which song belongs to which alarm.
15. The song area does not feel like a music library.

Solve all of these.

==================================================
4. HIGH-LEVEL INFORMATION ARCHITECTURE
==================================================

The redesigned screen should have approximately this structure:

HEADER
    AriaAlarm
    supporting text / subtle status

NEXT ALARM HERO
    next alarm name
    time
    day/repeat information
    assigned song
    animated visual treatment
    quick enable/disable control

ALARMS
    section header
    Add Alarm
    list of alarm cards

MUSIC
    section header
    song library / uploaded songs
    Add Song

BOTTOM NAVIGATION

The exact structure can adapt to the existing application's navigation architecture.

The important thing is:

The next upcoming alarm should have the highest information priority.

==================================================
5. HEADER
==================================================

Current:

AriaAlarm

Upload your .mp3 files and start
your morning with a bang

Improve this.

Keep:

AriaAlarm

as the primary page title.

The subtitle should become shorter, more polished, and more useful.

Possible conceptual direction:

Your mornings, your way.

or:

Wake up to something worth hearing.

Do not blindly copy these phrases.

Choose wording appropriate to the product.

The header should not occupy excessive vertical space.

Use strong typography.

Maintain the elegant visual personality already established in the app.

==================================================
6. NEXT ALARM HERO
==================================================

This is the most important addition.

Instead of immediately showing a large generic "Alarm" settings card, make the next upcoming enabled alarm the HERO.

Example conceptual layout:

NEXT ALARM

06:25

Morning Run

Song 1

Tomorrow · Every weekday

[ enabled control ]

The exact information depends on what the existing data model supports.

The time should be large.

The alarm name should be clearly visible.

The song should be immediately associated with the alarm.

The next alarm should visually feel more important than other alarms.

==================================================
7. NEXT ALARM VISUALIZATION
==================================================

Create a subtle animated visual treatment around the next alarm.

This does not need to be a traditional clock.

Possible visual language:

- soft orange ambient glow
- subtle circular rings
- waveform-inspired lines
- slow breathing gradient
- extremely subtle particles
- animated arc indicating time/proximity to the alarm

The animation should be restrained.

It should create the feeling that the alarm is "waiting."

Do NOT make it look like a gaming dashboard.

==================================================
8. NEXT ALARM COUNTDOWN
==================================================

Where useful, show a countdown.

For example:

06:25

in 8h 43m

or:

Tomorrow · 8h 43m

Only implement a countdown if the existing product requirements and data model support it properly.

The countdown should update efficiently.

Avoid continuously recomputing the entire screen.

==================================================
9. NEXT ALARM ANIMATION
==================================================

The hero should have a subtle ambient animation while an alarm is enabled.

For example:

- glow slowly expands and contracts
- circular ring slowly rotates or moves
- waveform subtly responds to the current song
- time remains stable

The user's attention should be drawn to the alarm without creating visual noise.

When the alarm becomes disabled:

- glow fades
- ambient animation slows/stops
- status becomes visually subdued

==================================================
10. ALARM MODEL
==================================================

The application currently appears to have a single alarm.

Change the architecture so that alarms become independent entities.

Each alarm should conceptually contain:

Alarm
- id
- name
- hour
- minute
- enabled
- songId / song reference
- repeat configuration, if supported
- createdAt / updatedAt where appropriate

Use names appropriate to the existing codebase.

Do not duplicate models if an alarm model already exists.

Modify the existing model rather than creating an unnecessary second model.

==================================================
11. MULTIPLE ALARMS
==================================================

The user must be able to create multiple alarms.

The UI should display alarms in a clean list.

Example:

ALARMS                                  +

┌──────────────────────────────┐
│ 06:25     Morning Run        │
│           Song1              │
│           Weekdays            │
│                         ON    │
└──────────────────────────────┘

┌──────────────────────────────┐
│ 07:30     University         │
│           Song2               │
│           Mon–Fri             │
│                         ON    │
└──────────────────────────────┘

Do not create enormous cards.

Multiple alarms must remain easy to scan.

==================================================
12. ALARM CARD DESIGN
==================================================

Each alarm card should have:

- time
- alarm name
- assigned song
- repeat schedule if supported
- enabled/disabled state
- edit affordance
- deletion affordance
- optional next-occurrence information

The time is the primary visual element.

The name is the secondary visual element.

The song and repeat schedule are tertiary information.

Do not make every property look equally important.

==================================================
13. ALARM CARD VARIATION
==================================================

The next upcoming alarm can have a slightly elevated visual treatment.

For example:

- brighter surface
- subtle orange glow
- stronger typography
- small "NEXT" label

Other alarms can remain visually quieter.

This creates a natural hierarchy.

Avoid giving every alarm the same visual weight.

==================================================
14. ENABLE/DISABLE
==================================================

Every alarm needs its own enabled state.

Do not use a single global Enabled switch.

The enabled state should belong to the individual alarm.

The control can be:

- compact switch
- custom toggle
- accessible button

Choose whatever best fits the design system.

When enabled:

- subtle orange accent
- clear active state

When disabled:

- neutral/dimmed state

Do not make disabled alarms disappear.

They should remain visible and editable.

==================================================
15. ENABLE/DISABLE ANIMATION
==================================================

When the user toggles an alarm:

Enabled → Disabled

Animate:

- switch movement
- accent fade
- subtle card tone change
- hero state if this is the next alarm

Disabled → Enabled

Animate:

- switch movement
- accent appearing
- subtle highlight
- update "next alarm" if applicable

Do not animate the entire screen.

==================================================
16. CREATE NEW ALARM
==================================================

Add a clear:

+

Add Alarm

action.

The plus button should feel intentional.

Avoid placing a random floating orange circle in empty space.

It should belong naturally to the section/header.

Possible design:

ALARMS                        +

or:

ALARMS
                        [ + ]

Make the interaction obvious.

==================================================
17. ADD ALARM FLOW
==================================================

Tapping Add Alarm should open an elegant creation interface.

Prefer a dedicated screen or polished bottom sheet depending on the existing architecture.

Do NOT use a primitive default dialog containing every setting at once.

The creation flow should emphasize:

1. Alarm name
2. Time
3. Song
4. Repeat
5. Enabled

The order should follow the user's mental model.

==================================================
18. ALARM NAME
==================================================

Every alarm can have a custom name.

Example:

Morning Run
University
Gym
Wake Up
Weekend
Study
Early Flight

The name field should be visually prominent.

Placeholder:

Alarm name

Do not force generic names such as "Alarm 1".

If the user leaves it blank, generate a reasonable fallback such as:

Alarm

or:

Morning Alarm

depending on the app's conventions.

==================================================
19. TIME PICKER
==================================================

The time picker should feel integrated with the application's design.

Do not simply drop in a default Android component without styling consideration.

The selected time should be the dominant element.

Support the device's appropriate 12/24-hour preference.

Make the time visually large and easy to adjust.

==================================================
20. CUSTOM SONG PER ALARM
==================================================

THIS IS A CORE NEW FEATURE.

Each alarm can have its own song.

Example:

Morning Run
06:25
Song 1

University
07:10
Song 2

Weekend
09:00
Song X

An alarm must store a reference to its selected song.

Do NOT store the audio itself inside every alarm.

Use a reusable song entity/library and reference it from an alarm.

==================================================
21. SONG LIBRARY
==================================================

The existing screen has:

Songs (1)

and:

Song 1

Turn this into a real song library.

The library should allow:

- add/import song
- view uploaded songs
- preview song
- select song for an alarm
- delete song
- understand which song is selected

The UI should be compact.

Do not allow the song library to dominate the alarm management screen.

==================================================
22. UPLOAD / IMPORT SONG
==================================================

Preserve the existing .mp3 import functionality.

Add a polished:

Add Song

or:

+

action.

Use Android's appropriate document/file picker mechanism.

The user should be able to select an MP3 from device storage.

After import:

1. Validate file.
2. Persist the file safely.
3. Extract/display useful metadata where feasible.
4. Add it to the song library.
5. Make it available for alarm assignment.

Do not break existing imported songs.

==================================================
23. SONG PREVIEW
==================================================

Each song should have a small play/preview control.

Example:

▶  Song 1

The control should animate when playing.

Possible animation:

- icon changes play → pause
- subtle waveform movement
- progress indicator

Do not turn every song into a full media-player card.

A compact preview is sufficient.

==================================================
24. SONG SELECTION FOR ALARM
==================================================

When creating or editing an alarm:

Tap:

Song

Then open a song selection interface.

Show:

- song title
- artist if available
- play/preview
- currently selected state

Selected song should have a clear but restrained active state.

For example:

✓ selected

or:

orange indicator

Do NOT rely only on color.

==================================================
25. SONGLESS ALARMS
==================================================

Handle the case where no song has been assigned.

Do not break.

Display:

No song selected

and provide:

Choose a song

The alarm should remain editable.

The exact behavior when it rings should follow the application's product requirements.

Do not invent unsupported behavior.

==================================================
26. EDIT ALARM
==================================================

Tapping an alarm should open an edit screen/sheet.

Allow editing:

- name
- time
- song
- repeat schedule
- enabled state

Changes should save cleanly.

Use explicit Save where appropriate.

Avoid accidental modifications from navigating around the UI.

==================================================
27. DELETE ALARM
==================================================

Users need a way to delete an alarm.

Prefer:

Swipe

or:

Edit → Delete

or both.

If using swipe-to-delete:

- reveal delete action progressively
- use a clear deletion affordance
- animate the card out
- provide undo where appropriate

Do not make deletion too easy to trigger accidentally.

==================================================
28. EMPTY STATE — NO ALARMS
==================================================

If the user has zero alarms:

Do NOT show a huge empty box.

Instead create an intentional empty state.

Example:

No alarms yet

Create your first alarm
and wake up to your own soundtrack.

[ + Create Alarm ]

The empty state should be visually elegant and compact.

==================================================
29. EMPTY STATE — NO SONGS
==================================================

If there are no songs:

Your music library is empty.

Add an MP3
to personalize your alarms.

[ Add Song ]

Do not leave a giant blank region.

==================================================
30. PRIMARY INFORMATION HIERARCHY
==================================================

At a glance, the user should understand:

1. What is my next alarm?
2. What time will it ring?
3. What song will play?
4. Is it enabled?
5. What other alarms do I have?
6. What songs are available?

The screen should not force the user to inspect multiple cards to answer these.

==================================================
31. REDUCE VISUAL REPETITION
==================================================

The existing screen repeats:

- rounded rectangles
- large dark boxes
- orange circles
- orange icons
- outlined controls

Reduce this dramatically.

Use hierarchy through:

- scale
- typography
- spacing
- tonal surfaces
- iconography
- selective accent usage

Not every component needs a border.

==================================================
32. ORANGE COLOR SYSTEM
==================================================

Orange should become semantic.

Orange means:

- active
- selected
- primary action
- important state
- confirmation

Neutral tones mean:

- normal content
- secondary controls
- inactive state

Do not color every icon orange.

For example:

Song play icon → neutral

Song selected state → orange

Alarm enabled → orange

Alarm disabled → muted gray

Add button → orange

Delete → restrained destructive treatment

This will immediately make the screen clearer.

==================================================
33. ICONOGRAPHY
==================================================

Use icons intentionally.

Do not use the same orange treatment for every icon.

Suggested semantic icon system:

Alarm:
alarm/clock icon

Song:
music note

Add:
plus

Delete:
trash

Repeat:
repeat

Enabled:
appropriate active-state indicator

Settings:
gear

Icons should be:

- simple
- consistent
- appropriately sized
- accessible
- visually secondary to key information

==================================================
34. VISUALIZATION / MUSIC IDENTITY
==================================================

Consider introducing a subtle audio-inspired visual element.

For example:

- waveform
- equalizer bars
- circular audio ring

This should appear near the currently selected/next alarm song.

It could animate gently while previewing a song.

It should NOT become a giant decorative equalizer.

Its job is to connect:

alarm
+
music

==================================================
35. ALARM ACTIVATION STATE
==================================================

When an alarm is enabled and is the next upcoming alarm:

The UI should communicate:

READY

without needing a giant label.

Possible subtle indicators:

- small orange status dot
- active glow
- "NEXT" label
- enabled switch
- animated accent

Use multiple cues but keep them subtle.

==================================================
36. ALARM RINGING EXPERIENCE
==================================================

Inspect the existing application to determine how alarms are currently triggered.

Do not replace working alarm scheduling logic unnecessarily.

However, the new alarm architecture must support multiple scheduled alarms.

Ensure each individual alarm can:

- be scheduled
- be cancelled
- be rescheduled after edits
- be disabled
- trigger its assigned song

When an alarm is edited:

old schedule must be correctly cancelled/updated.

When an alarm is deleted:

its scheduled event must be removed.

When an alarm is disabled:

its scheduled event must no longer fire.

When it is enabled:

schedule it correctly.

Do not leave stale scheduled alarms.

==================================================
37. AUDIO PLAYBACK
==================================================

Each alarm should play its assigned custom song.

Use the existing audio implementation if one exists.

Do not create duplicate media-player infrastructure without reason.

Ensure:

- selected song is resolved correctly
- file access remains valid
- playback handles app/device state appropriately
- the alarm can play without the user leaving the app open, according to Android's supported mechanisms and the existing app architecture

Respect Android lifecycle and audio behavior.

==================================================
38. DATA MODEL / PERSISTENCE
==================================================

The new feature requires persistent alarm data.

Do not only store alarms in temporary UI state.

Determine the application's current persistence approach.

If the project already uses a database/backend:

integrate alarms and songs through the existing architecture.

If a local database is already present:

reuse it.

If Supabase is being used for user data:

follow the existing repository/data-layer architecture.

Do not put database queries directly inside the UI.

==================================================
39. BACKEND / DATABASE
==================================================

If the existing application stores user-specific data remotely, consider the necessary schema/data changes.

Conceptually:

songs
- id
- user_id
- name
- artist
- file reference
- metadata
- created_at

alarms
- id
- user_id
- name
- hour
- minute
- enabled
- song_id
- repeat configuration
- created_at
- updated_at

Do NOT blindly implement this exact schema.

First inspect the existing data model and adapt it to the application's architecture.

Add migrations where required.

Do not break existing users/data.

==================================================
40. FILE STORAGE
==================================================

Imported songs must be stored safely.

Determine how the current project stores uploaded MP3 files.

Preserve that approach if it is already sound.

If the current application uses local storage:

ensure the references remain valid across app restarts.

If cloud storage is used:

reuse the existing storage architecture.

Do not duplicate audio files unnecessarily.

==================================================
41. MIGRATION
==================================================

The existing app may have one alarm rather than multiple.

Create a migration strategy if required.

Existing alarm data should not disappear.

For example:

existing alarm
→ becomes one Alarm entity

existing selected song
→ remains available

Do not require the user to rebuild their alarm manually after the update.

==================================================
42. ANIMATION PHILOSOPHY
==================================================

The screen should feel alive.

However, animation must communicate state and interaction.

Do not animate everything continuously.

Use animation for:

- opening the screen
- selecting an alarm
- toggling enabled state
- adding an alarm
- deleting an alarm
- selecting a song
- previewing music
- updating the next alarm
- countdown changes
- playing/pausing music

Ambient animations should be extremely subtle.

==================================================
43. SCREEN ENTRY ANIMATION
==================================================

When opening AriaAlarm:

Header:
fade/slide in subtly.

Next alarm:
appear slightly after header.

Alarm list:
stagger gently.

Music:
appear after alarms.

Do not make the entire screen bounce into existence.

Suggested total duration:

approximately 500–900ms.

==================================================
44. ADD ALARM ANIMATION
==================================================

When tapping +:

The interface should transition naturally into the creation flow.

Possible sequence:

1. Plus button responds.
2. Creation screen/sheet expands.
3. Fields appear sequentially or as one polished composition.
4. Time is immediately visible.
5. Save action is clear.

The user should feel that they are "creating an alarm", not configuring a system settings page.

==================================================
45. ADD ALARM SAVE ANIMATION
==================================================

When an alarm is saved:

- creation view closes smoothly
- new alarm appears in list
- card fades/slides into position
- next-alarm status updates if necessary
- assigned song becomes visible
- enable state is immediately clear

If it becomes the next alarm:

animate the hero transition.

==================================================
46. SONG SELECTION ANIMATION
==================================================

When selecting a song:

- selected state appears smoothly
- play control changes state
- selection indicator animates
- song assignment updates

Do not refresh the entire screen abruptly.

==================================================
47. PLAYBACK ANIMATION
==================================================

When previewing a song:

Play button:
play → pause

Optional:
small waveform/equalizer animates

When playback stops:

waveform settles.

Avoid continuous expensive animations.

==================================================
48. DELETE ANIMATION
==================================================

When deleting an alarm:

- card moves with gesture if swipe-to-delete
- deletion confirmation if necessary
- card exits smoothly
- remaining alarms close the gap
- hero recalculates if the deleted alarm was the next alarm

If the application supports undo:

show an elegant undo action.

==================================================
49. NEXT ALARM TRANSITION
==================================================

This is important.

Suppose the current next alarm is:

06:25 Morning Run

and the user disables it.

The next enabled alarm might become:

07:30 University

The hero should update gracefully rather than jumping immediately.

Animate:

old alarm
→
new alarm

This creates the feeling of a living schedule.

==================================================
50. RESPONSIVE DESIGN
==================================================

Do not build the screen around the exact attached screenshot dimensions.

Support:

- small Android phones
- tall phones
- different aspect ratios
- different densities
- text scaling
- accessibility settings

Do not use large amounts of absolute positioning.

Everything should adapt naturally.

==================================================
51. ACCESSIBILITY
==================================================

Ensure:

- minimum touch target sizes
- content descriptions
- readable text
- adequate contrast
- accessible enable/disable controls
- accessible song selection
- accessible alarm editing
- accessible delete controls

Do not communicate enabled/disabled state only through orange/gray.

There must be a semantic state indication.

==================================================
52. DARK MODE DESIGN
==================================================

The app currently uses a dark theme.

Preserve it.

Do not use pure black for every surface.

Use subtle differences:

background
→
surface
→
elevated surface

This creates depth.

Avoid making every component look like a separate floating card.

==================================================
53. TYPOGRAPHY
==================================================

Use typography to communicate hierarchy.

Page title:
strong

Next alarm time:
very prominent

Alarm name:
medium/strong

Song:
secondary

Metadata:
small

Section headings:
medium/semibold

Buttons:
clear and concise

Do not use too many font weights.

Maintain the elegant visual personality already present in the app.

==================================================
54. SPACING SYSTEM
==================================================

Use a consistent spacing system such as:

4dp
8dp
12dp
16dp
20dp
24dp
32dp

Major sections should have clear breathing room.

But eliminate the huge unused spaces visible in the current screen.

The goal is:

SPACIOUS

not:

EMPTY.

==================================================
55. CARD SYSTEM
==================================================

Use fewer large containers.

Prefer:

- compact alarm rows
- one hero treatment
- compact music rows
- subtle surfaces

Do not put everything inside giant rounded rectangles.

==================================================
56. BOTTOM NAVIGATION
==================================================

The existing bottom navigation should remain unless the current architecture strongly suggests otherwise.

Preserve navigation destinations.

Redesign it to match the new visual language.

AriaAlarm should have a clearly recognizable selected state.

Avoid making the navigation excessively tall.

==================================================
57. INFORMATION DENSITY
==================================================

The current screen is too empty.

Increase useful information density without creating clutter.

The user should be able to see multiple alarms at once.

For example:

ALARMS

06:25
Morning Run
Song 1
Weekdays                         ON

07:30
University
Song 2
Mon–Fri                          ON

09:00
Weekend
Song 3
Sat–Sun                          OFF

This gives the screen substance without making it crowded.

==================================================
58. SECTION STRUCTURE
==================================================

Use clear section headings:

Next alarm

Alarms

Music

Avoid too many headings.

Each section should have a purpose.

Possible:

Alarms                         +

Music                          +

Use "See all" only when the section genuinely leads to a larger screen.

==================================================
59. MUSIC LIBRARY DESIGN
==================================================

Do not make the song library look like a giant file-management interface.

Use compact rows.

Example:

♫  Song 1
   Aaron Smith
                         ▶

or:

♫  Song 1                    ▶
    Aaron Smith

Keep song information easy to scan.

==================================================
60. ALARM CREATION UX
==================================================

The user should be able to create an alarm quickly.

The happy path should be:

Tap +
→ choose/set time
→ name alarm
→ choose song
→ choose repeat
→ save

Avoid forcing the user through unnecessary steps.

Defaults should be sensible.

For example:

Enabled:
ON

Song:
most recently used / no song depending on current product logic

Name:
Alarm

Repeat:
one-time or existing default

Do not invent behavior that conflicts with the existing application.

==================================================
61. VALIDATION
==================================================

Validate:

- valid time
- valid alarm name length
- song existence
- scheduling state
- repeat configuration

Show errors close to the affected control.

Do not use generic error dialogs when an inline message would work.

==================================================
62. ERROR HANDLING
==================================================

Handle:

- song import failure
- inaccessible song file
- deleted song assigned to alarm
- scheduling failure
- persistence failure
- missing data
- duplicate operations

Errors should be understandable to normal users.

Do not expose technical stack traces or database errors in the UI.

==================================================
63. PERFORMANCE
==================================================

Be careful with:

- audio previews
- repeated list recompositions
- continuous animations
- waveform rendering
- countdown timers
- database observers

Avoid unnecessary work.

Animations must remain smooth on mid-range devices.

Audio playback should not leak resources.

Cancel observers and coroutines appropriately.

==================================================
64. STATE MANAGEMENT
==================================================

Do not duplicate state.

The source of truth should be the existing repository/data architecture.

For example:

alarm edited
→ repository updates
→ UI observes new alarm
→ scheduler updates
→ next alarm recalculates
→ hero updates.

Do not manually change five unrelated pieces of local state.

==================================================
65. SCHEDULING ARCHITECTURE
==================================================

Inspect the existing alarm scheduling implementation.

Modify it so multiple independent alarms can exist.

Each alarm should be independently schedulable.

Required behaviors:

CREATE:
schedule alarm.

ENABLE:
schedule alarm.

DISABLE:
cancel that alarm's schedule.

EDIT:
cancel old schedule and schedule updated configuration.

DELETE:
cancel schedule and remove alarm.

REBOOT / APP RESTART:
ensure enabled alarms are restored appropriately according to the existing architecture and Android's supported scheduling mechanisms.

Do not create duplicate alarm events.

==================================================
66. SONG-ALARM RELATIONSHIP
==================================================

An alarm references a song.

Do not duplicate song metadata inside every alarm.

If the song is deleted:

Handle alarms referencing it gracefully.

Possible behavior:

Alarm remains
+
Song becomes:
No song selected

OR use the existing product's preferred behavior.

Choose a consistent strategy.

==================================================
67. SECURITY / DATA ISOLATION
==================================================

If the app supports accounts:

Ensure alarms and songs belong to the correct user.

Do not allow one user's data to appear in another user's data.

Reuse the existing authentication and data-access architecture.

==================================================
68. DO NOT BREAK EXISTING FEATURES
==================================================

Preserve:

- existing MP3 upload capability
- existing navigation
- existing audio functionality
- existing alarm behavior
- existing theme
- existing settings
- existing backend integration

The redesign must be an evolution of the existing application.

==================================================
69. DO NOT ADD UNNECESSARY DEPENDENCIES
==================================================

Before adding any dependency:

Check whether the functionality can be implemented using:

- existing project dependencies
- Android SDK
- current architecture
- existing utility classes

Only introduce a dependency when there is a clear technical benefit.

==================================================
70. DESIGN SYSTEM
==================================================

Create/reuse centralized design tokens for:

- colors
- typography
- spacing
- corner radius
- elevation
- animation durations

Do not scatter arbitrary values throughout the UI.

This screen should be maintainable.

==================================================
71. COMPONENT ARCHITECTURE
==================================================

Use modular components.

Conceptually:

AriaAlarmScreen
├── AriaAlarmHeader
├── NextAlarmHero
│   ├── AlarmTime
│   ├── AlarmName
│   ├── AlarmSong
│   └── AmbientAlarmVisualization
├── AlarmSection
│   ├── SectionHeader
│   └── AlarmList
│       └── AlarmCard
├── MusicSection
│   ├── SectionHeader
│   └── SongList
│       └── SongItem
└── BottomNavigation

Creation/editing:

AlarmEditor
├── AlarmNameField
├── TimeSelector
├── SongSelector
├── RepeatSelector
└── EnableToggle

Song management:

SongLibrary
SongItem
SongPreview

Use names appropriate to the project's architecture.

Do not create a giant all-in-one screen file.

==================================================
72. MODULARITY
==================================================

The alarm editor should be reusable for:

Create Alarm
Edit Alarm

Do not duplicate the entire UI.

Likewise:

Song selector
should be reusable for:
- assigning a song
- browsing songs

==================================================
73. EMPTY / LOADING / ERROR STATES
==================================================

Implement all important states.

ALARM LIST:
loading
populated
empty
error

SONG LIST:
loading
populated
empty
error

NEXT ALARM:
enabled
none scheduled

Do not leave blank white/black space when data is unavailable.

==================================================
74. VISUAL FEEDBACK
==================================================

Every meaningful action should have immediate feedback.

Create alarm:
→ new item appears

Delete alarm:
→ item exits

Enable:
→ active visual state

Disable:
→ subdued state

Select song:
→ selected state

Play:
→ playback feedback

Save:
→ confirmation through UI transition

Navigation:
→ clear destination change

==================================================
75. USE MOTION TO EXPLAIN THE UI
==================================================

Animations should explain relationships.

Examples:

Adding an alarm:
plus → new alarm

Selecting a song:
song row → assigned song field

Disabling next alarm:
active hero → next alarm replacement

Deleting:
alarm row → removed

Do not animate random elements independently.

==================================================
76. PREMIUM DETAILS
==================================================

Add polish through:

- subtle gradients
- small accent glows
- elegant dividers
- careful typography
- small status indicators
- smooth easing
- consistent icon alignment
- carefully chosen spacing

Avoid overdesign.

The interface should look premium because of craftsmanship, not because of visual effects.

==================================================
77. REFERENCE IMAGE
==================================================

The attached screenshot is the reference for the CURRENT implementation.

Use it to understand:

- existing layout
- existing content
- current typography personality
- current color identity
- current navigation
- current song functionality
- current alarm functionality

Do not simply redesign it by moving the same boxes around.

The objective is:

AriaAlarm 2.0

not:

AriaAlarm with prettier colors.

==================================================
78. BEFORE CODING
==================================================

First inspect:

- current AriaAlarm screen
- alarm model
- song model
- persistence
- MP3 import code
- audio playback code
- scheduling code
- backend
- navigation
- theme
- bottom navigation

Then determine the minimum set of files that must change.

Do not immediately start rewriting files.

==================================================
79. IMPLEMENTATION PHASES
==================================================

PHASE 1
Inspect architecture and current implementation.

PHASE 2
Document current alarm/song/data flow internally.

PHASE 3
Design updated models/data structures.

PHASE 4
Implement multiple-alarm persistence.

PHASE 5
Implement per-alarm names.

PHASE 6
Implement per-alarm song assignment.

PHASE 7
Implement per-alarm enabled state.

PHASE 8
Update alarm scheduling.

PHASE 9
Redesign AriaAlarm visual foundation.

PHASE 10
Build Next Alarm Hero.

PHASE 11
Build alarm list.

PHASE 12
Build alarm creation/editing flow.

PHASE 13
Build music library/selection flow.

PHASE 14
Implement animations.

PHASE 15
Implement empty/loading/error states.

PHASE 16
Integrate navigation.

PHASE 17
Test the complete application.

==================================================
80. TEST MULTIPLE ALARMS
==================================================

Explicitly test:

Alarm A:
06:25
Morning Run
Song A
Enabled

Alarm B:
07:30
University
Song B
Enabled

Alarm C:
09:00
Weekend
Song C
Disabled

Verify:

- all appear
- each can be edited
- each can be enabled/disabled independently
- each references the correct song
- each schedules independently
- disabling A reveals B as next alarm
- deleting B does not affect A/C
- editing C does not alter A/B

==================================================
81. TEST SONG ASSIGNMENT
==================================================

Create at least several songs.

Assign:

Alarm A → Song A
Alarm B → Song B
Alarm C → Song C

Verify they remain correctly associated after:

- restarting app
- editing alarm
- disabling alarm
- enabling alarm
- navigating away
- reopening screen

==================================================
82. TEST SONG DELETION
==================================================

If a song is deleted:

verify alarms referencing it behave according to the defined fallback behavior.

Do not crash.

==================================================
83. TEST EXISTING DATA
==================================================

If there is an existing single alarm in the application:

verify it survives the upgrade/migration.

Existing MP3s must also survive.

Do not reset existing data.

==================================================
84. TEST VISUAL RESPONSIVENESS
==================================================

Test:

- small screen
- large/tall screen
- normal font
- larger accessibility font

Ensure:

- time does not clip
- song names do not overflow badly
- alarm cards remain readable
- buttons remain accessible
- next alarm hero remains balanced

==================================================
85. TEST REDUCED MOTION
==================================================

Where possible, respect reduced-motion preferences.

If reduced motion is enabled:

- minimize ambient animations
- preserve functionality
- retain state feedback
- avoid continuous movement

==================================================
86. PERFORMANCE TEST
==================================================

Ensure:

- scrolling remains smooth
- audio preview starts/stops cleanly
- no memory leaks
- no excessive recomposition
- no unnecessary database calls
- no runaway timers
- no continuously running animation when screen is not visible

==================================================
87. FINAL VISUAL QUALITY BAR
==================================================

The finished screen should feel like a premium alarm application.

Look for these qualities:

CALM
The interface should feel peaceful.

PERSONAL
Each alarm should feel like the user's own.

CLEAR
The user should instantly understand what happens next.

ALIVE
The UI should subtly react.

ELEGANT
The design should not look like stock Android.

USEFUL
Every major visual element should communicate information or provide an action.

==================================================
88. IMPORTANT THINGS TO AVOID
==================================================

Do NOT:

- create one massive alarm card per alarm
- use orange for every icon
- use orange for every switch
- create excessive borders
- fill the screen with gradients
- use excessive glow
- use giant animations
- constantly animate the page
- use fake alarm data
- hard-code songs
- break existing MP3 functionality
- create duplicate audio systems
- create duplicate backend clients
- put database logic directly in the UI
- use temporary-only alarm state
- lose existing alarms during migration
- create a generic Android alarm clock design
- make the interface resemble a settings screen

==================================================
89. FINAL VALIDATION CHECKLIST
==================================================

Before finishing, verify:

UI:
- Does the screen no longer feel empty?
- Is the next alarm immediately obvious?
- Are multiple alarms easy to scan?
- Is each alarm clearly associated with its song?
- Are enabled/disabled states obvious?
- Is the interface visually balanced?
- Is orange used strategically?
- Are cards no longer repetitive?

UX:
- Can a user create an alarm quickly?
- Can they name it?
- Can they choose a song?
- Can they edit it?
- Can they enable/disable it?
- Can they delete it?
- Can they manage multiple alarms?
- Can they preview songs?
- Can they add more songs?

Motion:
- Does the next alarm feel alive?
- Do actions have tactile feedback?
- Do list changes animate naturally?
- Does the UI react to changes rather than jumping?

Technical:
- Does every alarm persist?
- Does each alarm schedule independently?
- Does disabling cancel scheduling?
- Does editing reschedule correctly?
- Does deleting remove scheduling?
- Does the assigned song persist?
- Does existing data survive?
- Does existing MP3 functionality still work?
- Does the project compile?
- Does the application run?

==================================================
90. FINAL REPORT
==================================================

After implementing everything, provide:

1. Files changed
2. Components created
3. Data models changed
4. Database/backend changes
5. Scheduling changes
6. Song-management changes
7. New user interactions
8. Animation changes
9. Migration strategy
10. Dependencies added
11. Testing performed
12. Known limitations/issues

Do not merely tell me what should be done.

Actually implement it in the project.

The final implementation should be production-quality and should make AriaAlarm feel like a major upgrade of the existing application.