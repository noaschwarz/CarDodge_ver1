import java.util.Properties
import java.io.FileInputStream
import java.io.File

val localProperties = Properties()
val rootPropertiesFile = File(rootDir, "local.properties")
val modulePropertiesFile = File(projectDir, "local.properties")

if (rootPropertiesFile.exists()) {
    rootPropertiesFile.inputStream().use { localProperties.load(it) }
} else if (modulePropertiesFile.exists()) {
    modulePropertiesFile.inputStream().use { localProperties.load(it) }
}

val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY") ?: "AIzaSy_Placeholder_Key_Since_File_Not_Found"


plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cardodge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cardodge"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_KEY", "\"$mapsApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}