// kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.api.tasks.bundling.Jar

plugins {
    kotlin("jvm") version "2.3.21"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("org.openrewrite.rewrite") version "7.36.0"
}
group = "io.dedyn.engineermantra"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.json:json:20260522")
    implementation("ch.qos.logback:logback-classic:1.5.34")
    implementation("net.dv8tion:JDA:6.4.2")
    implementation("club.minnced:discord-webhooks:0.8.4")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    implementation("net.java.dev.jna:jna:5.18.1")
    implementation("net.java.dev.jna:jna-platform:5.18.1")
    implementation("net.sourceforge.tess4j:tess4j:5.17.0")
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:6.1.0")
}

sourceSets {
    main {
        kotlin.srcDir("$projectDir/src/kotlin")
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "io.dedyn.engineermantra.omega.Main"
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

val fatJar = tasks.register<Jar>("fatJar") {
    duplicatesStrategy = DuplicatesStrategy.WARN
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = "1.0"
        attributes["Main-Class"] = "io.dedyn.engineermantra.omega.Main"
    }

    // include runtime classpath (dependencies)
    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })

    // include project's jar contents
    from({
        val jarFile = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        if (jarFile.exists()) zipTree(jarFile) else emptyList<Any>()
    })

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    dependsOn(tasks.named("jar"))
    archiveClassifier.set("all")
}

tasks.named("build") {
    dependsOn(fatJar)
}
