import com.codingfeline.buildkonfig.compiler.FieldSpec.Type

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sentry)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.touchlab.skie)
    alias(libs.plugins.realm)
    alias(libs.plugins.moko.resources)
}

group = "org.datepollsystems.waiterrobot.shared"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCRefinement")
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
            languageSettings.optIn("org.orbitmvi.orbit.annotation.OrbitExperimental")
            languageSettings.optIn("kotlin.RequiresOptIn")
        }

        commonMain.dependencies {
            // Logger
            api(libs.touchlab.kermit)

            // Dependency injection
            implementation(libs.koin.core)

            // Architecture
            api(libs.orbit.core) // MVI
            api(libs.moko.mvvm) // ViewModelScope
            implementation(libs.touchlab.skie.annotations)

            // Localization
            api(libs.moko.resources)

            // Permissions
            api(libs.moko.permissions)

            // Ktor (HTTP client)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.encoding)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)

            // Realm (Database)
            implementation(libs.realm)

            // SharedSettings
            implementation(libs.settings)
            implementation(libs.settings.coroutines)

            // Helper
            api(libs.kotlinx.datetime)
            // Also needed by android for ComposeDestination parameter serialization
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            // Dependency injection
            api(libs.koin.android)

            // Ktor (HTTP client)
            implementation(libs.ktor.client.cio)
        }

        iosMain.dependencies {
            // Ktor (HTTP client)
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = group.toString()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

buildkonfig {
    packageName = "$group.buildkonfig"
    defaultConfigs {
        buildConfigField(Type.STRING, "sharedVersion", version as String, const = true)
    }
}

skie {
    analytics {
        disableUpload.set(true)
        enabled.set(false)
    }
}

detekt {
    source.from(
        "src/androidMain/kotlin",
        "src/commonMain/kotlin",
        "src/iosMain/kotlin",
    )
}

multiplatformResources {
    resourcesPackage.set("$group.localization")
}
