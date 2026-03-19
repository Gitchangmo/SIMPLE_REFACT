plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.cookandroid.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cookandroid.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.firebase:firebase-auth:23.2.1")
    implementation("com.google.firebase:firebase-firestore:25.1.2")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // 3/1 추가
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    //api관련
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    // 필터링
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.foundation:foundation:1.3.1")


    // 통계
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")


    // 파이어베이스 메시지 (최신 24.1.1)
    implementation ("com.google.firebase:firebase-messaging:23.4.1")

    // 이메일 인증 관련
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))


    // 챗봇 관련
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // flexlayout
    implementation ("com.google.android.flexbox:flexbox:3.0.0")


}