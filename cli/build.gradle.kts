/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import io.spine.internal.dependency.Clikt
import io.spine.internal.dependency.spine.Logging
import io.spine.internal.gradle.publish.SpinePublishing

plugins {
    application
    `version-to-resources`
    `write-manifest`
    `build-proto-model`
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

dependencies {
    listOf(
        kotlin("reflect"),
        Clikt.lib,
        Logging.lib,
        Logging.libJvm,
    ).forEach { implementation(it) }

    listOf(
        ":api",
        ":cli-api",
        ":backend",
        ":java"
    ).forEach { implementation(project(it)) }

    testImplementation(project(":test-env"))
}

/** The publishing settings from the root project. */
val spinePublishing = rootProject.the<SpinePublishing>()

/** Use the same prefix for naming application files as for published artifacts. */
val appName = spinePublishing.artifactPrefix.replace("-", "")

/** The names of the published modules defined the parent project. */
val modules: Set<String> = spinePublishing.modules

/**
 * A callback for distribution archive tasks that prepends [appName] to the file name,
 * if the file is an archive of a project module. Otherwise, the file name is intact.
 *
 * This is used to make archives with our code more visible and grouped together (by their names)
 * under the `lib` folder.
 */
fun addPrefixIfModule(fcd: FileCopyDetails) {
    val sourceName = fcd.sourceName
    val isModule = modules.any { sourceName.startsWith("$it-") }
    if (isModule) {
        fcd.name = "$appName-$sourceName"
    }
}

tasks.distZip {
    archiveFileName.set("${appName}.zip")
    eachFile {
        addPrefixIfModule(this)
    }
}

tasks.distTar {
    archiveFileName.set("${appName}.tar")
    eachFile {
        addPrefixIfModule(this)
    }
}

application {
    mainClass.set("io.spine.protodata.cli.app.MainKt")
    applicationName = appName
}

tasks.getByName<CreateStartScripts>("startScripts") {
    windowsStartScriptGenerator = ScriptGenerator { _, _ -> /* Do nothing. */ }
    val template = resources.text.fromFile("$projectDir/launch.template.py")
    (unixStartScriptGenerator as TemplateBasedScriptGenerator).template = template
}

/**
 * A task re-packing the distribution ZIP archive into a JAR.
 *
 * Some Maven repositories, particularly GitHub Packages, do not support publishing arbitrary files.
 * This way, we trick it to accept this file (as a JAR).
 */
val setupJar by tasks.registering(Jar::class) {
    from(zipTree(tasks.distZip.get().archiveFile))
    from("$projectDir/install.sh")

    archiveFileName.set("${appName}.jar")

    dependsOn(tasks.distZip)
}

val stagingDir = "$buildDir/staging"

val stageProtoData by tasks.registering(Copy::class) {
    from(zipTree(setupJar.get().archiveFile))
    into(stagingDir)

    dependsOn(setupJar)
}

val protoDataLocationProperty = "protoDataLocation"

tasks.register("installProtoData", Exec::class) {
    val cmd = mutableListOf("$stagingDir/install.sh")
    if (rootProject.hasProperty(protoDataLocationProperty)) {
        cmd.add(rootProject.property(protoDataLocationProperty)!!.toString())
    }
    commandLine(cmd)
    dependsOn(stageProtoData)
}

/**
 * Create a configuration for re-archived distribution ZIP so that it later can be used
 * for the publication.
 */
val setupArchiveConfig = "setupArchive"
configurations.create(setupArchiveConfig)
artifacts {
    add(setupArchiveConfig, setupJar)
}

publishing {
    val pGroup = project.group.toString()
    val pVersion = project.version.toString()

    publications {
        create("setup", MavenPublication::class) {
            groupId = pGroup
            artifactId = "$appName-setup"
            version = pVersion

            setArtifacts(project.configurations.getAt(setupArchiveConfig).allArtifacts)
        }
        create("fat-jar", MavenPublication::class) {
            groupId = pGroup
            artifactId = "$appName-fat-cli"
            version = pVersion

            artifact(tasks.shadowJar) {
                // Avoid `-all` suffix in the published artifact.
                // We cannot remove the suffix by setting the `archiveClassifier` for
                // the `shadowJar` task because of the duplication check for pairs
                // (classifier, artifact extension) performed by `ValidatingMavenPublisher` class.
                classifier = ""
            }
        }
    }
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    mergeServiceFiles("desc.ref")
    mergeServiceFiles("META-INF/services/io.spine.option.OptionsProvider")
    isZip64 = true
    exclude(
        // Exclude license files that cause or may cause issues with LicenseReport.
        // We analyze these files when building artifacts we depend on.
        "about_files/**",
        "license/**",

        "ant_tasks/**", // `resource-ant.jar` is of no use here.

        /* Exclude `https://github.com/JetBrains/pty4j`.
          We don't need the terminal. */
        "resources/com/pty4j/**",

        // Protobuf files.
        "google/**",
        "spine/**",
        "src/**",

        // Java source code files of the package `org.osgi`.
        "OSGI-OPT/**"
    )
}

// See https://github.com/johnrengelman/shadow/issues/153.
tasks.shadowDistTar.get().enabled = false
tasks.shadowDistZip.get().enabled = false
