import org.gradle.jvm.tasks.Jar

plugins {
    java
}

group = "com.user404_"
val baseVersion = "1.4"

// Determine if this is a release build (property -Prelease=true)
val isRelease = project.hasProperty("release") && project.property("release") == "true"

// Get the current Git commit hash (short)
val commitId = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim().takeIf { it.isNotBlank() } ?: "unknown"

// Set the final version
version = if (isRelease) {
    baseVersion
} else {
    "$baseVersion-preview+$commitId"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.0.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Process plugin.yml to inject the version
tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// ===== GENERATED SOURCES CONFIGURATION =====
val generatedSrcDir = layout.buildDirectory.dir("generated/sources/buildConfig/java/main").get().asFile

// Add the generated sources directory to the main source set
sourceSets.main.get().java.srcDir(generatedSrcDir)

// Task that writes BuildConfig.java
val generateBuildConfig = tasks.register("generateBuildConfig") {
    val buildConfigFile = File(generatedSrcDir, "com/user404_/balsync/BuildConfig.java")
    outputs.file(buildConfigFile)
    doLast {
        buildConfigFile.parentFile.mkdirs()
        buildConfigFile.writeText(
            """
            package com.user404_.balsync;
            public final class BuildConfig {
                public static final String VERSION = "${project.version}";
                public static final String COMMIT_ID = "${commitId}";
                public static final String BUILD_TYPE = "${if (isRelease) "release" else "preview"}";
                public static final boolean UPDATE_CHECKER_ENABLED = ${isRelease};
            }
            """.trimIndent()
        )
    }
}

// Make compileJava depend on the generation task
tasks.compileJava.get().dependsOn(generateBuildConfig)
// =============================================

tasks.jar {
    archiveBaseName.set("BalSync")
}

// Fat-jar (shade) task
val shadeJar = tasks.register<Jar>("shadeJar") {
    archiveBaseName.set("BalSync")
    archiveClassifier.set("all")
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.build {
    dependsOn(shadeJar)
}