// Root-level build.gradle.kts

plugins {
    id("com.android.application") version "8.12.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
