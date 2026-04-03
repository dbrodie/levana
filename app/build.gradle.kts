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
    namespace = "com.levana.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.levana.app"
        minSdk = 34
        targetSdk = 35
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
            val rel = signingConfigs.getByName("release")
            signingConfig = if (rel.storeFile?.exists() == true &&
                                rel.storePassword != null &&
                                rel.keyAlias      != null &&
                                rel.keyPassword   != null) rel
                            else signingConfigs.getByName("debug")
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
            }
        }
    }
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
    implementation(libs.datastore.preferences)
    implementation(libs.play.services.location)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.matching { it.name == "testReleaseUnitTest" }.configureEach {
    enabled = false
}
