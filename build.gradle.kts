import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    kotlin("jvm") version "2.4.0-RC"
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("Magnet-Universal-1.16.5-1.21.x.jar")
    }

    runServer {
        val selectedVersion = providers.gradleProperty("runMinecraftVersion").getOrElse("1.21.11")
        minecraftVersion(selectedVersion)
        runDirectory.set(layout.projectDirectory.dir("run/$selectedVersion"))
        if (selectedVersion == "1.16.5") {
            legacyPluginLoading()
            ignoreUnsupportedJvm()
        }
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

fun RunServer.configureMagnetRun(minecraftVersion: String) {
    this.minecraftVersion(minecraftVersion)
    pluginJars(tasks.shadowJar.flatMap { it.archiveFile })
    jvmArgs("-Xms2G", "-Xmx2G")
}

tasks.register<RunServer>("runLegacyServer") {
    group = "run paper"
    description = "Run Paper 1.16.5 with the universal Magnet jar."
    configureMagnetRun("1.16.5")
    runDirectory.set(layout.projectDirectory.dir("run/legacy-1.16.5"))
    legacyPluginLoading()
    ignoreUnsupportedJvm()
}

tasks.register<RunServer>("runModernServer") {
    group = "run paper"
    description = "Run Paper 1.21.11 with the universal Magnet jar."
    configureMagnetRun("1.21.11")
    runDirectory.set(layout.projectDirectory.dir("run/modern-1.21.11"))
}
