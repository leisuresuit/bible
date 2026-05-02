import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.skie)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val absApiKey = localProperties.getProperty("abs.api.key") ?: ""

kotlin {
    androidLibrary {
        namespace = "org.tjc.bible"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        
        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
            linkerOpts.add("-lsqlite3")
        }
    }
    
    sourceSets {
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.navigation3.ui)
            
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.sqldelight.coroutines.ext)
            implementation(libs.skie.annotations)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

sqldelight {
    databases {
        create("BibleDatabase") {
            packageName.set("org.tjc.bible.cache")
        }
    }
}

val generateConfig = tasks.register("generateConfig") {
    val outputDir = layout.buildDirectory.dir("generated/kotlin/config/commonMain/kotlin")
    val apiKey = absApiKey
    inputs.property("absApiKey", apiKey)
    outputs.dir(outputDir)
    doLast {
        val configFile = outputDir.get().file("org/tjc/bible/Config.kt").asFile
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """
            package org.tjc.bible

            object Config {
                const val ABS_API_KEY = "$apiKey"
            }
            """.trimIndent()
        )
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin/config/commonMain/kotlin"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateConfig)
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
