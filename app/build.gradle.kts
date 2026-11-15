plugins {
 alias(libs.plugins.android.application)
 id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}
android {
 namespace = "com.example.campilot"
 compileSdk = 37
 defaultConfig { applicationId = "com.example.campilot"; minSdk = 28; targetSdk = 37; versionCode = 1; versionName = "0.1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 val releaseKey = rootProject.file(".signing/campilot-release.jks")
 val releasePasswordFile = rootProject.file(".signing/password.txt")
 if (releaseKey.exists() && releasePasswordFile.exists()) {
  signingConfigs.create("submission") {
   storeFile = releaseKey
   storePassword = releasePasswordFile.readText().trim()
   keyAlias = "campilot"
   keyPassword = storePassword
  }
 }
 buildTypes {
  getByName("release") {
   isDebuggable = false
   isMinifyEnabled = false
   signingConfig = signingConfigs.findByName("submission")
  }
 }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
dependencies {
 implementation("ai.pleos.playground:Vehicle:2.3.2")
 implementation("ai.pleos.playground:NaviHelper:3.0.5")
 implementation(platform("androidx.compose:compose-bom:2026.09.00"))
 implementation("androidx.activity:activity-compose:1.12.4")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("androidx.datastore:datastore-preferences:1.2.0")
 implementation("androidx.navigation3:navigation3-ui:1.0.1")
 implementation(libs.androidx.appcompat)
 implementation(libs.material)
 testImplementation(libs.junit)
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
}