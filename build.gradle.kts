import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
    }
}
plugins {
    kotlin("jvm") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    //id "com.github.ben-manes.versions" version "0.20.0"
    //id 'com.sedmelluq.jdaction' version '1.0.2'
}
group = "io.dedyn.engineermantra"

repositories {
    //jcenter()
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

//val compile by configurations.creating

// In this section you declare the dependencies for your production and test code
dependencies {
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = "1.8.20")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "1.6.4")
    implementation(group = "org.json", name = "json", version = "20230227")
    implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.6")
    implementation(group = "net.dv8tion", name = "JDA", version = "5.0.0-beta.11")
    implementation(group = "club.minnced", name = "discord-webhooks", version = "0.8.2")
    implementation(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "3.1.4")
    implementation(group = "net.java.dev.jna", name = "jna", version = "5.13.0")
    implementation(group = "net.java.dev.jna", name = "jna-platform", version = "5.13.0")
    implementation(group = "net.sourceforge.tess4j", name = "tess4j", version = "5.7.0")
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