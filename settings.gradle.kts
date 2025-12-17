rootProject.name = "ChattORE"
include("common")
include("chattore")
include("chattore-agent")
project(":chattore-agent").projectDir = file("agent")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://repo.extendedclip.com/releases/")
    }
}
