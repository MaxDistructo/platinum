import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    }
}
plugins {
    kotlin("jvm") version "2.2.0"
}
group = "io.dedyn.engineermantra"

repositories {
    //jcenter()
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

//val compile by configurations.creating

// In this section you declare the dependencies for your production and test code
dependencies {
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = "2.2.0")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.10.2")
    implementation(group = "org.json", name = "json", version = "20250107")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.5.18")
    //Due to bugs in the latest beta, we want to pull the latest commit from Jitpack instead of Maven.
    //implementation(group = "com.github.discord-jda", name = "JDA", version = "79b1b560b1")
    implementation(group = "net.dv8tion", name = "JDA", version = "5.6.1")
    implementation(group = "club.minnced", name = "discord-webhooks", version = "0.8.4")
    implementation(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "3.5.4")
    implementation(group = "net.java.dev.jna", name = "jna", version = "5.17.0")
    implementation(group = "net.java.dev.jna", name = "jna-platform", version = "5.17.0")
    implementation(group = "net.sourceforge.tess4j", name = "tess4j", version = "5.16.0")
    implementation(group = "edu.cmu.sphinx", name = "sphinx4-core", version = "5prealpha-SNAPSHOT")
    implementation(group = "edu.cmu.sphinx", name = "sphinx4-data", version = "5prealpha-SNAPSHOT")
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:6.1.0")
    /**
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mustache")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    */
//implementation(kotlin("stdlib-jdk8"))
}

sourceSets {
    main {
        kotlin.srcDir("$projectDir/src/kotlin")
    }
}

/*
sourceSets["main"].withConvention(conventionType = org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class){
    kotlin.srcDir("$projectDir/src/kotlin")
}
*/

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "io.dedyn.engineermantra.omega.Main"
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    "build" {
        dependsOn(fatJar)
    }
}
val fatJar = task("fatJar", type = Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.WARN
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = "1.0"
        attributes["Main-Class"] = "io.dedyn.engineermantra.omega.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    with(tasks.jar.get() as CopySpec)
}
