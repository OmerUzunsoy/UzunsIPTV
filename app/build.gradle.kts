plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // DÜZELTME BURADA: Versiyon numarasını ekledik
    id("com.google.devtools.ksp") version "2.0.21-1.0.26"
}

android {
    namespace = "com.uzuns.uzunsiptv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uzuns.uzunsiptv"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        lint {
            abortOnError = false
            checkReleaseBuilds = false
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Temel Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Retrofit & Gson (İnternet)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ExoPlayer (Video)
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")

    // Glide (Resim)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ViewModel (Veri Yönetimi)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // ROOM DATABASE (VERİTABANI)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Coroutines desteği
    ksp("androidx.room:room-compiler:$room_version") // İşlemci (ksp kullanıyoruz)

    // YouTube Player
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Testler
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
