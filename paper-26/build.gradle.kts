plugins {
    java
}

// Paper 26.1 requires Java 25.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

sourceSets {
    main {
        // Same sources as :core; only the platform package is replaced by the 26.1 implementation.
        java.setSrcDirs(listOf("../core/src/main/java", "src/main/java"))
        resources.setSrcDirs(listOf("../core/src/main/resources"))
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }
}

val pluginVersion = version.toString()

tasks.processResources {
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("plugin.yml") {
        expand(mapOf("version" to pluginVersion))
    }
}

tasks.jar {
    archiveBaseName.set("Tradewind")
    archiveClassifier.set("paper-26.1")
}

