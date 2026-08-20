plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") // required now — separate from org.jetbrains.compose
    id("org.jetbrains.compose")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
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
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.javax.inject)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(libs.kotlinx.datetime)
}