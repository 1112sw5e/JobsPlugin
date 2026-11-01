import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.api.file.DuplicatesStrategy

allprojects {
    group = "com.tradewind"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }

    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

