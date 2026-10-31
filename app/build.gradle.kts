import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

// ── Version (git tag via VERSION_NAME in CI; `git describe` locally) ─────────
fun gitDescribeVersion(): String? = runCatching {
    val proc = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
        .directory(rootProject.projectDir)
        .start()
    proc.inputStream.bufferedReader().readLine()?.removePrefix("v")?.trim()
        ?.takeIf { it.matches(Regex("""\d+\.\d+\.\d+""")) }
}.getOrNull()

val tagVersion = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() }
    ?: gitDescribeVersion()
val appVersionCode = tagVersion
    ?.split(".")
    ?.map { it.toInt() }
    ?.let { (maj, min, pat) -> maj * 10000 + min * 100 + pat }
    ?: 1
val appVersionNameRelease = tagVersion ?: "0.0.0-dev"

// ── Signing (env vars for CI; keystore.properties for local release builds) ──
val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { keystoreProps.load(it) }

fun signingProp(envKey: String, propKey: String): String? {
    val envVal = System.getenv(envKey)
    if (!envVal.isNullOrBlank()) return envVal
    val propVal = keystoreProps.getProperty(propKey)
    if (!propVal.isNullOrBlank()) return propVal
    return null
}

android {
    namespace = "ninja.doskey.app.levana"
    compileSdk = 36
    // Pinned for reproducible release builds (docs/fdroid/README.md)
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "ninja.doskey.app.levana"
        minSdk = 34
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionNameRelease
    }

    signingConfigs {
        named("debug") {
            // uses the default ~/.android/debug.keystore
        }
        create("release") {
            storeFile     = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            storePassword = signingProp("KEYSTORE_PASSWORD", "storePassword")
            keyAlias      = signingProp("KEY_ALIAS",         "keyAlias")
            keyPassword   = signingProp("KEY_PASSWORD",      "keyPassword")
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Without a release keystore the APK is left unsigned, the way F-Droid builds it for
            // the reproducible-build comparison (docs/fdroid/README.md). -Plevana.debugSignRelease=true
            // signs it with the debug key instead, for installing a local release build.
            val rel = signingConfigs.getByName("release")
            signingConfig = when {
                rel.storeFile?.exists() == true &&
                    rel.storePassword != null &&
                    rel.keyAlias      != null &&
                    rel.keyPassword   != null -> rel
                providers.gradleProperty("levana.debugSignRelease").orNull == "true" ->
                    signingConfigs.getByName("debug")
                else -> null
            }
            // The commit hash file (META-INF/version-control-info.textproto) depends on how the
            // source was checked out; leave it out so any checkout of the tag builds the same APK.
            vcsInfo.include = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // No Google-encrypted dependency list in the APK signing block or the bundle (F-Droid, TASK-53)
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        jniLibs {
            // Ship the prebuilt .so files from AARs as they are. Without this AGP strips them only
            // when an NDK happens to be installed, so the APK would depend on the build machine.
            keepDebugSymbols += "**/*.so"
        }
    }

    sourceSets {
        // Exported Room schemas for MigrationTestHelper. Robolectric only reads the
        // variant's merged assets, so they ship in debug builds but never in release.
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                // Opt-in foldable layout audit; see ScreenshotTest.Audit
                listOf("levana.audit.size", "levana.audit.fontScale", "levana.audit.dir").forEach { key ->
                    providers.gradleProperty(key).orNull?.let { value -> it.systemProperty(key, value) }
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Screenshot baselines are committed; tests write to the same directory (see ScreenshotTest)
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.kosherjava.zmanim)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.navigation.compose)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.acra.mail)
    implementation(libs.acra.dialog)
    implementation(libs.datastore.preferences)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    kspTest(libs.androidx.room.compiler)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.matching { it.name == "testReleaseUnitTest" }.configureEach {
    enabled = false
}

// KSP writes newly exported Room schemas into the debug assets directory above.
tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn("kspDebugKotlin")
}
