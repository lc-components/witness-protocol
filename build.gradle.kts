import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    val kotlinVersion = "1.3.50"
    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
}

repositories {
    jcenter()
    maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    mavenCentral()
}

val kotlinVersion = "1.3.70"

plugins {
    kotlin("jvm") version "1.3.70"
    id("kotlinx-serialization") version ("1.3.72")
}

group = "de.bcoding.ltc"
version = "1.0-SNAPSHOT"

val klockVersion = "1.10.5"
val kotlinSerializationVersion = "0.20.0"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.commons:commons-lang3:3.7")
    implementation("com.google.guava:guava:28.0-jre")
    implementation("com.google.protobuf:protobuf-java:3.9.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.0.1")
    testImplementation("org.assertj:assertj-core:3.12.2")
    testImplementation("io.mockk:mockk:1.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime$kotlinSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$kotlinSerializationVersion")
    implementation("com.tinder.statemachine:statemachine:0.2.0")
    implementation("com.soywiz.korlibs.klock:klock-jvm:$klockVersion")

    implementation("com.github.jershell:kbson:0.2.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}