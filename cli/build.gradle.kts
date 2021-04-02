/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import io.spine.gradle.internal.Deps

plugins {
    application
}

dependencies {
    implementation(project(":compiler"))
    implementation(kotlin("reflect"))
    implementation("com.github.ajalt.clikt:clikt:3.1.0")

    implementation(Deps.build.flogger.lib)

    testImplementation(project(":testutil"))
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

/**
 * A task re-packing the distribution ZIP archive into a JAR.
 *
 * Some Maven repositories, particularly GitHub Packages, do not support publishing arbitrary files.
 * This way, we trick it to accept this file (as a JAR).
 */
val executableAsJar by tasks.registering(Jar::class) {
    from(zipTree(tasks.distZip.get().archiveFile))
    from("$projectDir/install.sh")

    archiveFileName.set("${appName}.jar")

    dependsOn(tasks.distZip)
}

val executableArchivesConfig = "executableArchives"

configurations.create(executableArchivesConfig)

artifacts {
    add(executableArchivesConfig, executableAsJar)
}

publishing {
    publications {
        create("exec", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "executable"
            version = project.version.toString()

            setArtifacts(project.configurations.getAt(executableArchivesConfig).allArtifacts)
        }
    }
}

val versionFile = "$buildDir/version/version.txt"

val createVersionFile by tasks.registering {

    inputs.property("version", project.version)
    outputs.file(versionFile)

    doLast {
        file(versionFile).writeText(project.version.toString())
    }
}

tasks.processResources {
    dependsOn(createVersionFile)
}
