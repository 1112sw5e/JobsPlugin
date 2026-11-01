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
    compileOnly("me.clip:placeholderapi:2.11.6")
    // WorldGuard 7.0.18+ ships Java 25 bytecode, so the Java 21 artifact compiles against 7.0.17
    // (same API). Transitive resolution is off: the POM pulls a server-side set the plugin
    // never links against at compile time.
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.17") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.2") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.4.2") { isTransitive = false }
    compileOnly("com.github.NEZNAMY:TAB-API:6.1.2")

    // Bundled: Paper ships no JDBC driver, and servers may run without Maven access.
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")

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
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    }
}

tasks.test {
    useJUnitPlatform()
}

