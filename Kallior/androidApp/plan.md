## 1. OVERLAY + APP BLOCKING (CORE LOOP) — STATUS: 80% COMPLETE, NEEDS POLISH
What is working:

- AppBlockerForegroundService monitors UsageStats every 1 second
- OverlayManager shows a blocking screen with animation (concentric rings + capybara)
- "Allow Until" dropdown (5/10/15 minutes)
- "Exit" button that sends user to home
- Glitch/distortion effect at the boundary

Critical gaps to fix:

### Overlay needs ANIMATION on entry (not just static appear):

- Currently just appears. Should scale + fade in smoothly (300ms)
- Add slide-in from bottom or grow from center

### "Allow Until" persistence is correct but no visual feedback:

- After user selects 5/10/15 mins, the button tap should give haptic feedback
- Small toast: "Unlocked for 10 minutes". On the top of the screen like a rectangular popup (with rounder corners which have corner radius equal to 22.5% of the breadth).

Code location: OverlayManager.kt lines 96-200. The BlockingOverlay composable (lines 214-397) is the UI.

Action: Add entrance animation + feedback:
```kotlin
// In BlockingOverlay, wrap the Box in:
@Composable
private fun BlockingOverlay(...) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
    ) {
        Box(...) { ... }
    }
}
```

## 2. FOCUS FORTRESS (BLOCKER UI) — STATUS: NEEDS ANIMATIONS
What's there:

- Beautiful lotus illustration with ripple animation
- App blocker status card
- Time sink / total screen time metrics
- App list, website list

Missing animations:

Metrics should animate in on entry:

- Time sink value should count up (e.g., 2h 34m slides in from 0)
- Earnings card should have the $ amount animate

## 3. ANDROID MANIFEST & PERMISSIONS — STATUS: CRITICAL
You need all of these or app blocker won't work:
```xml
<!-- In AndroidManifest.xml -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Service declarations -->
<service android:name="org.example.project.AppBlockerForegroundService"
    android:foregroundServiceType="dataSync" />
```

Check if already added, if not, add them.

## 4. PROGRESSION VISUAL (THE GAME FEEL) — STATUS: MINIMAL, NEEDS PRIORITY
This is what separates "app" from "game":

- When user completes a task → show a brief 'Consistency' rectangular pop-up at the top that changes from its initial value to the new value
- Similar implementation should be done for other fields like 'Discipline', 'Health', 'Resilience' and 'Focus'. The icons should also appear along with the fields and values.
- Display the values in whole numbers not decimals.
- The rectangular pop-up at the top should have rounded corners 22.5% of it's breadth.
- If multiple values changed, display them one by one like a slideshow automatically.
- There should be an animation of the values counting up/down from its initial value to the new value.
- Daily reset → show a "New Day, New Start!" in the rectangular popup.

Quick implementation: Add a FloatingPopup composable:
```kotlin
@Composable
fun FloatingXpPopup(xpAmount: Int, modifier: Modifier = Modifier) {
    val offsetY by animateFloatAsState(
        targetValue = -100f,
        animationSpec = tween(600)
    )
    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(600)
    )
    
    Text(
        text = "", // according to the situation
        modifier = modifier.offset(y = offsetY.dp).graphicsLayer { this.alpha = alpha },
        color = KalliorColors.AccentOrange,
        fontWeight = FontWeight.Bold
        // also add the rounded corners
    )
}
```
