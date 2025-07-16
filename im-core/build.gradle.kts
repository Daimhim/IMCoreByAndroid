import shadow.bundletool.com.android.tools.r8.internal.pU

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.zjkj.im_core"
    compileSdk = 33

    defaultConfig {
        minSdk = 22
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        manifestPlaceholders["processName"] = ":push"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        aidl = true
    }
}

group = "com.github.Daimhim"
version = "1.0.4.1"
dependencies {

//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.appcompat)
//    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    compileOnly(libs.gson)
    compileOnly(libs.imc.core)
    compileOnly(libs.okhttp)
    compileOnly(libs.timber)
    compileOnly(libs.contexthelper)
    compileOnly(libs.java.websocket)
    compileOnly("com.github.kongqw:NetworkMonitor:1.2.0")

    implementation(libs.androidx.work.runtime)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.Daimhim"
            artifactId = "IMCoreByAndroid"
            version = "1.0.4.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}