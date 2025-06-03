import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginYml)
}

group = "org.openredstone"
version = "1.1"

dependencies {
    implementation(project(":common"))
    implementation(libs.kotlinx.serialization.cbor)
    compileOnly(libs.paper)
    compileOnly(libs.papi)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

bukkit {
    name = "ChattoreAgent"
    main = "org.openredstone.chattore.agent.ChattoreAgent"
    apiVersion = "1.20"
    depend = listOf("PlaceholderAPI")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
