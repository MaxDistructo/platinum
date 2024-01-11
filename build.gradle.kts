import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}
plugins {
    kotlin("jvm") version "1.9.22"
    //id("com.github.johnrengelman.shadow") version "7.1.2"
    //id "com.github.ben-manes.versions" version "0.20.0"
    //id 'com.sedmelluq.jdaction' version '1.0.2'
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.22"
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
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = "1.9.22")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.7.3")
    implementation(group = "org.json", name = "json", version = "20231013")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.14")
    //Due to bugs in the latest beta, we want to pull the latest commit from Jitpack instead of Maven.
    //implementation(group = "com.github.discord-jda", name = "JDA", version = "79b1b560b1")
    implementation(group = "net.dv8tion", name = "JDA", version = "5.0.0-beta.19")
    implementation(group = "club.minnced", name = "discord-webhooks", version = "0.8.4")
    implementation(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "3.2.0")
    implementation(group = "net.java.dev.jna", name = "jna", version = "5.13.0")
    implementation(group = "net.java.dev.jna", name = "jna-platform", version = "5.13.0")
    implementation(group = "net.sourceforge.tess4j", name = "tess4j", version = "5.10.0")
    implementation(group = "edu.cmu.sphinx", name = "sphinx4-core", version = "5prealpha-SNAPSHOT")
    implementation(group = "edu.cmu.sphinx", name = "sphinx4-data", version = "5prealpha-SNAPSHOT")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mustache")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    kotlinOptions.freeCompilerArgs += "-Xjsr305=strict"
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "io.dedyn.engineermantra.omega.Main"
    }
    "build" {
        dependsOn(fatJar)
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = JavaVersion.VERSION_17.toString()
}

val fatJar = task("fatJar", type = Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = "1.0"
        attributes["Main-Class"] = "io.dedyn.engineermantra.omega.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}