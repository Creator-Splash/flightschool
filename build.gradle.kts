import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java")
    id("java-library")

    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.paperweight)
}

group = "com.a3v1k"
version = "0.01-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.lucko.me/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.helpch.at/releases/")
    maven("https://jitpack.io")
    maven("https://maven.devs.beer/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    // Paper
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly(libs.paper.api)

    // Local jars
    compileOnly(files("libs/CreatorSplashCore-1.0.0.jar"))

    // WorldEdit / WorldGuard
    compileOnly(libs.worldedit)
    compileOnly(libs.worldguard)

    // MythicMobs
    compileOnly(libs.mythicmobs)

    // ModelEngine
    compileOnly(libs.modelengine)

    // LuckPerms
    compileOnly(libs.luckperms)

    // PlaceholderAPI
    compileOnly(libs.papi)

    // ItemsAdder
    compileOnly(libs.itemsadder.api)

    // AdvancedReplay
    compileOnly(libs.advancedreplay)

    // Cloud Commands
    paperLibrary(libs.cloud.paper)
    paperLibrary(libs.cloud.annotations)

    // Development
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.jetbrains.annotations)

    annotationProcessor(libs.auto.service)
    compileOnly(libs.auto.service.annotations)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
        finalizedBy("exportJars")
    }

    shadowJar {
        archiveClassifier.set("")
        minimize()
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs = listOf("-parameters")
    }

    register<Copy>("exportJars") {
        val shadowJar = named<ShadowJar>("shadowJar")
        dependsOn(shadowJar)
        from(shadowJar.flatMap { it.archiveFile })
        into(rootProject.layout.projectDirectory.dir("target"))
    }
}

paper {
    name = "FlightSchool"
    version = "0.01-alpha"
    apiVersion = "1.21"
    main = "com.a3v1k.flightSchool.FlightSchool"
    generateLibrariesJson = true

    serverDependencies {
        register("CreatorSplashCore") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("MythicMobs") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("ModelEngine") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("WorldEdit") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("WorldGuard") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("LuckPerms") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("ItemsAdder") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
        }

        register("TAB") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }

        register("AdvancedReplay") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}