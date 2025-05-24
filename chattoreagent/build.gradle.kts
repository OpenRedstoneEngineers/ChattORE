import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

group = "org.openredstone"
version = "1.1"

repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation(project(":common"))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-cbor", version = "1.8.0")
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.17-R0.1-SNAPSHOT")
    compileOnly(group = "me.clip", name = "placeholderapi", version = "2.11.6")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
