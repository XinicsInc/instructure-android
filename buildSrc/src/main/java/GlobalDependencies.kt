@file:Suppress("unused")

object Versions {
    /* SDK Versions */
    const val COMPILE_SDK = 26
    const val MIN_SDK = 21
    const val TARGET_SDK = 26

    /* Build/tooling */
    const val ANDROID_GRADLE_TOOLS = "3.0.1"
    const val BUILD_TOOLS = "26.0.2"

    /* Testing */
    const val ATSL_ORCHESTRATOR = "1.0.2-alpha1"
    const val JACOCO = "0.8.0"
    const val JUNIT = "4.12"
    const val ROBOLECTRIC = "3.3.2"

    /* Kotlin */
    const val KOTLIN = "1.2.21"
    const val KOTLIN_ANKO = "0.10.1"
    const val KOTLIN_COROUTINES = "0.18"

    /* Google, Play Services */
    const val GOOGLE_SERVICES = "3.0.0"
    const val OSS_LICENSES_PLUGIN = "0.9.1"
    const val PLAY_SERVICES = "11.4.2"
    const val SUPPORT_LIBRARY = "26.1.0"

    /* Others */
    const val APOLLO = "0.4.4"
    const val CRASHLYTICS = "2.6.8@aar"
    const val PICOCLI = "2.3.0"
    const val PSPDFKIT = "4.4.0"
}

object Libs {
    /* Kotlin */
    const val KOTLIN_STD_LIB = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.KOTLIN}"
    const val KOTLIN_COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KOTLIN_COROUTINES}"
    const val KOTLIN_COROUTINES_ANDROID = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.KOTLIN_COROUTINES}"

    /* Apollo/GraphQL */
    const val APOLLO_RUNTIME = "com.apollographql.apollo:apollo-runtime:${Versions.APOLLO}"
    const val APOLLO_ANDROID_SUPPORT = "com.apollographql.apollo:apollo-android-support:${Versions.APOLLO}"
    const val APOLLO_HTTP_CACHE = "com.apollographql.apollo:apollo-http-cache:${Versions.APOLLO}"

    /* Support Libs */
    const val SUPPORT_ANNOTATION = "com.android.support:support-annotations:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_APPCOMPAT = "com.android.support:appcompat-v7:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_CARDVIEW = "com.android.support:cardview-v7:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_DESIGN = "com.android.support:design:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_PALETTE = "com.android.support:palette-v7:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_PERCENT = "com.android.support:percent:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_RECYCLERVIEW = "com.android.support:recyclerview-v7:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_V13 = "com.android.support:support-v13:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_V4 = "com.android.support:support-v4:${Versions.SUPPORT_LIBRARY}"
    const val SUPPORT_VECTOR = "com.android.support:support-vector-drawable:${Versions.SUPPORT_LIBRARY}"

    /* Play Services */
    const val PLAY_SERVICES = "com.google.android.gms:play-services-gcm:${Versions.PLAY_SERVICES}"
    const val PLAY_SERVICES_ANALYTICS = "com.google.android.gms:play-services-analytics:${Versions.PLAY_SERVICES}"
    const val PLAY_SERVICES_OSS_LICENSES = "com.google.android.gms:play-services-oss-licenses:${Versions.PLAY_SERVICES}"
    const val PLAY_SERVICES_WEARABLE = "com.google.android.gms:play-services-wearable:${Versions.SUPPORT_LIBRARY}"

    /* Testing */
    const val JUNIT = "junit:junit:${Versions.JUNIT}"
    const val ROBOLECTRIC = "org.robolectric:robolectric:${Versions.ROBOLECTRIC}"

    /* Other */
    const val CRASHLYTICS = "com.crashlytics.sdk.android:crashlytics:${Versions.CRASHLYTICS}"
    const val PICOCLI = "info.picocli:picocli:${Versions.PICOCLI}"
    const val PSPDFKIT = "com.pspdfkit:pspdfkit-demo:${Versions.PSPDFKIT}"
}

object Plugins {
    const val FABRIC = "io.fabric.tools:gradle:1.+"
    const val ANDROID_GRADLE_TOOLS = "com.android.tools.build:gradle:${Versions.ANDROID_GRADLE_TOOLS}"
    const val APOLLO = "com.apollographql.apollo:apollo-gradle-plugin:${Versions.APOLLO}"
    const val KOTLIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.KOTLIN}"
    const val OSS_LICENSES = "com.google.gms:oss-licenses:${Versions.OSS_LICENSES_PLUGIN}"
    const val GOOGLE_SERVICES = "com.google.gms:google-services:${Versions.GOOGLE_SERVICES}"
}
