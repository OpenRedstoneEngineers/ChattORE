import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

group = "org.openredstone"
version = "1.2"

repositories {
    mavenCentral()
    maven {
        name = "sonatype-oss"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        name = "velocity"
        url = uri("https://nexus.velocitypowered.com/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(group = "com.uchuhimo", name = "konf", version = "1.1.2")
    implementation(group = "com.google.code.gson", name = "gson", version = "2.10.1")
    implementation(group = "co.aikar", name = "acf-velocity", version = "0.5.1-SNAPSHOT")
    implementation(group = "org.javacord", name = "javacord", version = "3.8.0")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.58.0")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.58.0")
    implementation(group = "org.jetbrains.exposed", name = "exposed-java-time", version = "0.58.0")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.8.0")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-cbor", version = "1.8.0")
    implementation(group = "org.xerial", name = "sqlite-jdbc", version = "3.46.0.0")
    val jacksonVersion = "2.18.2"
    implementation(
        group = "com.fasterxml.jackson.dataformat",
        name = "jackson-dataformat-yaml",
        version = jacksonVersion
    )
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)
    compileOnly(group = "net.luckperms", name = "api", version = "5.1")
    compileOnly(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
    kapt(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.shadowJar {
    relocate("co.aikar.commands", "org.openredstone.chattore.acf")
    relocate("co.aikar.locales", "org.openredstone.chattore.locales")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
