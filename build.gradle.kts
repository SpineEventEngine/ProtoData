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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.PublishingRepos
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.spinePublishing
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)

    apply(from = "$rootDir/version.gradle.kts")

    val spineBaseVersion: String by extra

    dependencies {
        classpath("io.spine.tools:spine-mc-java:$spineBaseVersion")
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib)
    }
}

plugins {
    kotlin("jvm") version io.spine.internal.dependency.Kotlin.version
    id(io.spine.internal.dependency.Kotlin.Dokka.pluginId) version(io.spine.internal.dependency.Kotlin.Dokka.version)
    idea
    `integration-test`
}

spinePublishing {
    targetRepositories.add(PublishingRepos.gitHub("ProtoData"))
    projectsToPublish.addAll(
        "cli",
        "compiler",
        "protoc",
        "codegen-java"
    )
    spinePrefix.set(false)
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")

    group = "io.spine.protodata"
    version = extra["protoDataVersion"]!!

    repositories.applyStandard()
}

subprojects {

    apply {
        plugin("kotlin")
        plugin("idea")
        plugin(Kotlin.Dokka.pluginId)
    }

    val spineCoreVersion: String by extra

    dependencies {
        testImplementation("io.spine.tools:spine-testutil-server:$spineCoreVersion")
        testImplementation(kotlin("test-junit5"))
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }

    tasks.test {
        useJUnitPlatform()

        testLogging {
            events = setOf(PASSED, FAILED, SKIPPED)
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

    kotlin {
        explicitApi()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=kotlin.io.path.ExperimentalPathApi,kotlin.ExperimentalUnsignedTypes",
                "-Xinline-classes",
                "-Xjvm-default=all"
            )
        }
    }

    val dokkaJavadoc by tasks.getting(DokkaTask::class)

    tasks.register("javadocJar", Jar::class) {
        from(dokkaJavadoc.outputDirectory)
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
    }
}
