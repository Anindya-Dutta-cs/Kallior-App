# Running Kallos

## Android (device or emulator)

### Fix: “composeApp.main is not supported … Cannot obtain the package”

That error means the run configuration uses the **wrong module**.

| Wrong | Correct |
|-------|---------|
| `Kallos.composeApp.main` | `Kallos.composeApp` |

`*.main` is a Kotlin Multiplatform **source set**, not the Android application.

**Steps in Android Studio / IntelliJ:**

1. **Run → Edit Configurations…**
2. Delete or ignore **composeApp.main**.
3. Use the shared config **Kallos (Android)** (from `.run/Kallos Android.run.xml`) or create:
   - **+ → Android App**
   - **Module:** `Kallos.composeApp` (no `.main` suffix)
   - **Launch:** Default Activity
4. **File → Sync Project with Gradle Files**
5. Select a device/emulator and run.

### Command line (always works)

```shell
.\gradlew.bat :composeApp:installDebug
```

Then open the app on the device, or use **Kallos installDebug** from the run dropdown.

## Desktop (JVM)

Use **Kallos (Desktop)** or:

```shell
.\gradlew.bat :composeApp:run
```

Do **not** use an “Android App” configuration for desktop.

## iOS

Open the `iosApp` folder in Xcode and run on a simulator or device.

## Build fails with “not a regular file” (OneDrive)

If Gradle reports `Cannot snapshot … not a regular file` under `composeApp/src`, files may be **cloud-only** OneDrive placeholders.

1. In File Explorer, right-click the `Kallos` project folder → **Always keep on this device**.
2. Or move the repo outside OneDrive (e.g. `C:\dev\Kallos`).
3. Sync Gradle again, then run **Kallos (Android)**.
