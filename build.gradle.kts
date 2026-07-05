import org.gradle.kotlin.dsl.invoke
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sentry) apply false
    alias(libs.plugins.kotlin.detekt) apply true
    alias(libs.plugins.moko.resources) apply false
    alias(libs.plugins.axionRelease)
}

scmVersion {
    // Tags are plain semver (e.g. 3.1.1), no "v" prefix.
    tag {
        prefix.set("")
    }

    // A dirty working tree must never look like a clean release build.
    ignoreUncommittedChanges.set(false)

    // Non-release (between-tags) builds are the lava (staging/dev) builds. Mark them
    // as such and append the short commit hash so a build can be traced back to an
    // exact commit (e.g. in Sentry) -> e.g. 3.1.2-lava-a1b2c3d.
    // snapshotCreator only runs for snapshots, so clean release tags stay bare (3.1.1).
    snapshotCreator { _, position -> "-lava-${position.shortRevision}" }
}

// versionName (display) for all clients, derived from git tags via axion-release.
version = scmVersion.version

// versionCode (monotonic integer) required by Google Play and the App Store.
// Total commit count is strictly increasing on main. See README "Versioning schema"
// for the same-commit trade-off and how to force a bump.
val gitVersionCode: Int = 29300724 + providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toInt()

val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
    output = layout.buildDirectory.file("reports/detekt/merge.sarif")
}

allprojects {
    version = rootProject.version
    extra["versionCode"] = gitVersionCode

    apply(plugin = rootProject.libs.plugins.kotlin.detekt.get().pluginId)

    detekt {
        config.from(rootDir.resolve("detekt.yml"))
        buildUponDefaultConfig = true
        basePath = rootDir.path
        // Autocorrection can only be done locally
        autoCorrect = System.getenv("CI")?.lowercase() != true.toString()
    }

    dependencies {
        detektPlugins(rootProject.libs.detekt.formatting)
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            html.required = true
            sarif.required = true
        }
        finalizedBy(detektReportMergeSarif)
    }
    detektReportMergeSarif {
        input.from(tasks.withType<Detekt>().map { it.sarifReportFile })
    }
}