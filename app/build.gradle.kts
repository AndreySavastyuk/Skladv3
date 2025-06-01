
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.warehouseapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.warehouseapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Локальные библиотеки из папки libs
    implementation(files("libs/nlsblesdk.aar"))
    implementation(files("libs/onsemi_blelibrary.jar"))
    implementation(files("libs/onsemi_fotalibrary.jar"))
    implementation(files("libs/printer-lib-3.2.0.aar"))

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM and bundles
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Добавьте зависимости Hilt
    implementation("com.google.dagger:hilt-android:2.47")
    kapt("com.google.dagger:hilt-compiler:2.47")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room Database
    implementation(libs.bundles.room)
    kapt(libs.androidx.room.compiler)

    // Camera X bundle
    implementation(libs.bundles.camera)

    // ML Kit for barcode scanning
    implementation(libs.google.mlkit.barcode.scanning)

    // ZXing for QR code generation
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    // Network bundle
    implementation(libs.bundles.network)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.squareup.retrofit2:retrofit:2.9.0") // Или более новая версия
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Если используете Gson

}