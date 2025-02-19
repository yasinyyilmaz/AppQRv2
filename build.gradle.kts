// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false // veya en güncel sürüm
}

// Eklenti bağımlılıkları burada tanımlanır
buildscript {
    dependencies {
        classpath(libs.google.services) // veya en güncel sürüm
    }
}