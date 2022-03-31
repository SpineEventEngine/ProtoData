/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.internal.dependency.Flogger

plugins {
    application
    `version-to-resources`
    `build-proto-model`
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("7.1.2")
}

dependencies {
    implementation(project(":compiler"))
    implementation(kotlin("reflect"))
    implementation(Clikt.lib)
    implementation(Flogger.lib)
    runtimeOnly(Flogger.Runtime.systemBackend)

    testImplementation(project(":test-env"))
}

val appName = "protodata"

tasks.distZip {
    archiveFileName.set("${appName}.zip")
}

tasks.distTar {
    archiveFileName.set("${appName}.tar")
}

application {
    mainClass.set("io.spine.protodata.cli.MainKt")
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
}

afterEvaluate {
    val createVersionFile: Task by tasks.getting
    @Suppress("UNUSED_VARIABLE")
    val sourcesJar: Task by tasks.getting {
        dependsOn(createVersionFile)
    }
}

// See https://github.com/johnrengelman/shadow/issues/153.
tasks.shadowDistTar.get().enabled = false
tasks.shadowDistZip.get().enabled = false
