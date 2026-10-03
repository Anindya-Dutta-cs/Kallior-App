import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") // required now — separate from org.jetbrains.compose
    id("org.jetbrains.compose")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.app.kallior"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.app.kallior"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // local.properties is ignored by Git; user-level Gradle properties take precedence for CI.
    val localProperties = Properties().apply {
        rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
    }
    val supabaseUrl = providers.gradleProperty("SUPABASE_URL")
        .orElse(localProperties.getProperty("SUPABASE_URL") ?: "")
        .get()
    val supabasePublishableKey = providers.gradleProperty("SUPABASE_PUBLISHABLE_KEY")
        .orElse(localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY") ?: "")
        .get()
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabasePublishableKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose.v190)
    implementation(libs.androidx.core.ktx.v1131)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.health.connect.client)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.supabase.auth)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.javax.inject)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lottie.compose)
    // kotlin.test's @Test is a typealias to JUnit4 on the JVM; the bare
    // kotlin-test artifact only carries the assertions, so both parts are needed.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${libs.versions.kotlin.get()}")
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.datetime)
}
