plugins {
    java
}

// Java 21 is the floor for the whole 1.21.x line.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    main {
        // platform121 holds the 1.21 implementation of com.tradewind.platform.Platform.
        java.setSrcDirs(listOf("src/main/java", "src/platform121/java"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
}

dependencies {
    // Compiling against the API floor (1.21.1) keeps the jar loadable on every 1.21.x server.
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }

    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
    archiveClassifier.set("paper-1.21")
}

tasks.test {
    useJUnitPlatform()
}

