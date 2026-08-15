plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

android {
  namespace = "jp.girky.taskmanage.cephalonGTD"
  compileSdk {
    version = release(37)
  }
  
  defaultConfig {
    applicationId = "jp.girky.taskmanage.cephalonGTD"
    minSdk = 37
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
    
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  
  buildTypes {
    release {
      optimization {
        enable = false
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.material.kolor)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  // Serialization
  implementation(libs.kotlinx.serialization.json)

  // DataStore & Biometric
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.biometric)

  // Google AI Edge (AICore / Gemini Nano)
  implementation(libs.google.ai.edge.aicore)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}