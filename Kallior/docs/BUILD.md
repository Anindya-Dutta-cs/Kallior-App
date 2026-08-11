# Build environment (JDK)

## The JDK 25 error

If Gradle fails immediately with only `25` or `25.0.2` in the message, the **Gradle daemon** is running on JDK 25 while an **older Gradle** (8.14.x) is in use. That Gradle bundles Kotlin 2.0.x for parsing `*.gradle.kts`, which does not recognize Java 25 as a runtime.

This project uses **Gradle 9.2.1**, which supports **running the build on JDK 25**.

## Recommended setup

| Role | Version | Notes |
|------|---------|--------|
| **Run Gradle** (JAVA_HOME) | JDK **25** or **21** | Both work with Gradle 9.2.1+ |
| **Compile JVM/Android bytecode** | **JVM 21** target | Set in `composeApp/build.gradle.kts` (Kotlin + Android `compileOptions`) |
| **Android (AGP 8.13)** | JDK **17+** for Gradle; toolchain **21** | [AGP 8.13 defaults to JDK 17](https://developer.android.com/build/releases/agp-8-13-0-release-notes) |

You do **not** need to uninstall JDK 25. The toolchain downloads or selects JDK 21 for compilation while Gradle can stay on 25.

## If the build is still unstable on JDK 25

Install [JDK 21 LTS](https://adoptium.net/) and point Gradle at it:

**Windows (PowerShell, current session):**

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"
.\gradlew.bat :composeApp:compileKotlinJvm
```

**Or** set `JAVA_HOME` permanently in System Environment Variables.

## Verify

```shell
java -version
.\gradlew.bat --version
```

Expect `Launcher JVM` and `Daemon JVM` to show your installed JDK (21 or 25), and Gradle **9.2.1**.

## Common commands

```shell
.\gradlew.bat :composeApp:compileKotlinJvm
.\gradlew.bat :composeApp:assembleDebug
.\gradlew.bat :composeApp:run
```
