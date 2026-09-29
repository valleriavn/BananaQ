import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
}

fun quotedBuildConfigValue(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val supabaseUrl = localProperties.getProperty("SUPABASE_URL")
    ?: System.getenv("SUPABASE_URL").orEmpty()
val supabasePublishableKey = localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY")
    ?: System.getenv("SUPABASE_PUBLISHABLE_KEY").orEmpty()

android {
    namespace = "com.example.bananaq"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bananaq"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", quotedBuildConfigValue(supabaseUrl))
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            quotedBuildConfigValue(supabasePublishableKey)
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "src/main/keepRules/rules.keep"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    androidResources {
        noCompress += "tflite"
    }
    buildFeatures {
        buildConfig = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("BananaQ.apk")
        }
    }
}

dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.tensorflow.lite)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("com.google.code.gson:gson:2.14.0")
}
