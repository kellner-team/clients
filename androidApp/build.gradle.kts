import com.android.build.api.dsl.VariantDimension
import com.android.build.gradle.internal.dsl.NdkOptions.DebugSymbolLevel
import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import org.gradle.internal.extensions.core.extra
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.composeCompiler)
}

private val localProperties = Properties().apply {
    project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { load(it) }
}

fun fromProjectOrLocalProperties(name: String): Any? = run {
    project.findProperty(name) ?: localProperties.getOrDefault(name, null)
}

group = "org.datepollsystems.waiterrobot"

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "org.datepollsystems.waiterrobot.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = this@android.namespace

        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // Version is derived from git tags via axion-release (see root build.gradle.kts).
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.version.toString()

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        val keyPassword: String? = fromProjectOrLocalProperties("keyPassword")?.toString()
        val storePassword: String? = fromProjectOrLocalProperties("storePassword")?.toString()
        val keyStoreFile = file(".keys/app_sign.jks")

        // Only create signingConfig, when all needed configs are available
        if (keyPassword != null && storePassword != null && keyStoreFile.exists()) {
            create("release") {
                keyAlias = "WaiterRobot"
                storeFile = keyStoreFile
                this.keyPassword = keyPassword
                this.storePassword = storePassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            allowedHosts("*")
        }

        release {
            isMinifyEnabled = false // TODO enable proguard
            signingConfig = signingConfigs.findByName("release")
            ndk.debugSymbolLevel = DebugSymbolLevel.FULL.name
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    flavorDimensions += "environment"

    productFlavors {
        create("lava") {
            dimension = "environment"
            applicationIdSuffix = ".lava"
            manifestPlaceholders["host"] = "lava.kellner.team"
            allowedHosts("*")
        }

        create("prod") {
            dimension = "environment"
            manifestPlaceholders["host"] = "my.kellner.team"
            allowedHosts("my.kellner.team")
        }
    }

    applicationVariants.all variant@{
        // Include the generated navigation sources
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir(
                    File(
                        project.layout.buildDirectory.asFile.get(),
                        "/generated/ksp/$name/kotlin"
                    )
                )
            }
        }

        // Write built version to file after creating a bundle (needed for ci, to create the version tag)
        if (this.name.endsWith("Release")) {
            tasks.findByName(
                "publish${
                    this.name.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                }Bundle"
            )!!.doLast {
                File(project.layout.buildDirectory.asFile.get(), "version.tag")
                    .writeText(this@variant.versionName)
            }
        }
    }

    tasks.withType<KotlinCompilationTask<*>> {
        compilerOptions.freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

ksp {
    arg(
        "compose-destinations.codeGenPackageName",
        "org.datepollsystems.waiterrobot.android.generated.navigation"
    )
}

play {
    defaultToAppBundles.set(true)
    serviceAccountCredentials.set(file(".keys/service-account.json"))
    releaseStatus.set(ReleaseStatus.COMPLETED)
    track.set("internal")
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.location)

    coreLibraryDesugaring(libs.android.desugar)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.core)
    implementation(libs.androidx.compose.ui.graphics)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3.core)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material.icons.extended)

    // Compose helpers
    implementation(libs.accompanist.permissions)

    // Architecture (MVI)
    implementation(libs.orbit.compose)

    // Dependency injection
    implementation(libs.koin.compose) // Not aligned with other koin version

    // SafeCompose Navigation Args
    implementation(libs.compose.destinations)
    ksp(libs.compose.destinations.ksp)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)

    // QrCode Scanning
    implementation(libs.barcode.scanning)

    // In-App-Update support
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    // Stripe Tap-To-Pay
    implementation(libs.stripe.terminal)
    implementation(libs.stripe.ttp)
}

private fun VariantDimension.allowedHosts(vararg hosts: String) {
    buildConfigField(
        type = String::class.simpleName!!,
        name = "ALLOWED_HOSTS_CSV",
        value = hosts.joinToString(",", "\"", "\"")
    )
}
