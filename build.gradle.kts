import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.1.10"
    kotlin("jvm") version kotlinVersion
    id("com.gradleup.shadow") version "8.3.0"
    id("org.jetbrains.kotlin.kapt") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

group = ""
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
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "com.uchuhimo", name = "konf", version = "1.1.2")
    implementation(group = "net.luckperms", name = "api", version = "5.1")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.58.0")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.58.0")
    implementation(group = "org.jetbrains.exposed", name = "exposed-java-time", version = "0.58.0")
    implementation(group = "org.xerial", name = "sqlite-jdbc", version = "3.46.0.0")
    implementation(group = "co.aikar", name = "acf-velocity", version = "0.5.1-SNAPSHOT")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.6.0")
    implementation(group = "org.javacord", name = "javacord", version = "3.8.0")
    implementation(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.10.1")
    kapt(group = "com.velocitypowered", name = "velocity-api", version = "3.3.0-SNAPSHOT")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}

tasks.shadowJar {
    relocate("co.aikar.commands", "chattore.acf")
    relocate("co.aikar.locales", "chattore.locales")
    dependencies {
        exclude(
            dependency(
                "net.luckperms:api:.*"
            )
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
