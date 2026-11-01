import org.gradle.api.initialization.resolve.RepositoriesMode

// Auto-provisions JDK 21/25 when they are missing, so `./gradlew build`
// works on a machine with a single JDK installed.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
        maven("https://jitpack.io") { name = "jitpack" }
    }
}

rootProject.name = "tradewind"

// core      -> Paper 1.21.x artifact, Java 21 bytecode, compiled against the 1.21.1 API floor
// paper-26  -> Paper 26.1.x artifact, Java 25 bytecode, same sources compiled against 26.1.2
include("core")
include("paper-26")

