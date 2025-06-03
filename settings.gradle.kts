rootProject.name = "ChattORE"
include("common")
include("chattore")
include("chattoreagent")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://repo.extendedclip.com/releases/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}
