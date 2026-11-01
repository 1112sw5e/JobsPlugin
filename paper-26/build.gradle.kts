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
    compileOnly("me.clip:placeholderapi:2.11.6")
    // Same 7.0.17 API baseline as :core, so one shared source set compiles on both lines.
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.17") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.2") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.4.2") { isTransitive = false }
    compileOnly("com.github.NEZNAMY:TAB-API:6.1.2")

    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
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
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    }
}

