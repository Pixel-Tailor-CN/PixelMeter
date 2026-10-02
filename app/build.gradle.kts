plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val packageName = "vip.mystery0.pixel.meter"
val gitVersionCode: Int = providers.exec {
    commandLine(
        "git",
        "rev-list",
        "HEAD",
        "--count"
    )
}.standardOutput.asText.get().trim().toInt()
val gitVersionName: String =
    providers.exec {
        commandLine(
            "git",
            "rev-parse",
            "--short=8",
            "HEAD"
        )
    }.standardOutput.asText.get().trim()
val appVersionName: String = libs.versions.app.version.get()

private val androidLocaleQualifierRegex = Regex("""^(?:[a-z]{2,3}(?:-r[A-Z]{2})?|b\+[A-Za-z]{2,3}(?:\+[A-Za-z0-9]{2,8})*)$""")

private fun discoverLocaleFilters(resDirectory: java.io.File): List<String> {
    val defaultLocale = resDirectory.resolve("resources.properties")
        .takeIf { it.isFile }
        ?.readLines()
        ?.asSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("unqualifiedResLocale=") }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("Missing unqualifiedResLocale in app/src/main/res/resources.properties")

    val localizedLocales = resDirectory
        .listFiles { file -> file.isDirectory && file.name.startsWith("values-") }
        .orEmpty()
        .asSequence()
        .filter { it.resolve("strings.xml").isFile }
        .map { it.name.removePrefix("values-") }
        .filter { androidLocaleQualifierRegex.matches(it) }
        .sorted()
        .toList()

    return (listOf(defaultLocale) + localizedLocales).distinct()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

android {
    namespace = packageName
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt())
    }

    defaultConfig {
        applicationId = packageName
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = gitVersionCode
        versionName = appVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("sign")
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            versionNameSuffix = ".d$gitVersionCode.$gitVersionName"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            versionNameSuffix = ".r$gitVersionCode.$gitVersionName"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sign")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
    }
    @Suppress("UnstableApiUsage")
    androidResources {
        generateLocaleConfig = true
        localeFilters.addAll(discoverLocaleFilters(layout.projectDirectory.dir("src/main/res").asFile))
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)

    // Feature Dependencies
    implementation(libs.androidx.palette.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.zhanghai.preference)
    implementation(libs.skydoves.colorpicker)
    testImplementation(libs.junit)
}

apply(from = rootProject.file("signing.gradle"))
