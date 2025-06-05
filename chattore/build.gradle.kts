import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.buildconfig)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.acf)
    implementation(libs.javacord)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.sqliteJdbc)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.yaml)
    implementation(libs.jackson.kotlin)
    compileOnly(libs.luckperms)
    compileOnly(libs.velocity)
    kapt(libs.velocity)
}

buildConfig {
    packageName("${project.group}")
    buildConfigField("VERSION", provider { "${project.version}" })
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters = true
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
