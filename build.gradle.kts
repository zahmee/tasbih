plugins {
    id("com.android.application") version "9.3.0" apply false
    // AGP 9 compiles Kotlin natively; only the Compose compiler plugin is needed here.
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("androidx.room3") version "3.0.1" apply false
}
