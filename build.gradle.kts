import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems.jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.71"
    java
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))


    ////////////////////////////////////////////////////////////////////////TEST
    testCompile("org.jetbrains.kotlin:kotlin-test-junit:1.2.71")

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.2.71")
    ////////////////////////////////////////////////////////////////////////TEST
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.chl.audiomagic.MainKt"
    }

    // This line of code recursively collects and copies all of a project's files
    // and adds them to the JAR itself. One can extend this task, to skip certain
    // files or particular types at will
    from(
            configurations.runtime.map {
                if (it.isDirectory) it else zipTree(it)
            }
    )
}