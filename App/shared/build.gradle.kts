import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm() // lets you run/test the RAG pipeline tonight with plain `./gradlew :shared:run`

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            // enables the plain `main()` console demo in demo/ConsoleDemo.kt
        }
    }
}

android {
    namespace = "com.forgemind.shared"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Lets you run `./gradlew :shared:run` tonight to execute ConsoleDemo.kt
tasks.register<JavaExec>("run") {
    dependsOn("jvmMainClasses")
    mainClass.set("com.forgemind.demo.ConsoleDemoKt")
    classpath = files(
        kotlin.targets.getByName("jvm").compilations.getByName("main").output.allOutputs,
        configurations.getByName("jvmRuntimeClasspath")
    )
}
