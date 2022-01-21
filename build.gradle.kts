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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.RunBuild
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)

    apply(from = "$rootDir/version.gradle.kts")

    val mcJavaVersion: String by extra

    dependencies {
        classpath("io.spine.tools:spine-mc-java:$mcJavaVersion")
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib)
    }
}

val devProtoDataVersion: String by extra

plugins {
    kotlin("jvm")
    io.spine.internal.dependency.Dokka.apply {
        id(pluginId) version(version)
    }
    idea
    jacoco
    `force-jacoco`
}

spinePublishing {
    targetRepositories.addAll(
        PublishingRepos.gitHub("ProtoData"),
        PublishingRepos.cloudArtifactRegistry
    )
    projectsToPublish.addAll(
        "cli",
        "compiler",
        "protoc",
        "codegen-java",
        "testutil"
    )
    customPrefix.set("protodata-")
}

allprojects {
    apply {
        from("$rootDir/version.gradle.kts")
        plugin("idea")
        plugin("project-report")
    }

    group = "io.spine.protodata"
    version = extra["protoDataVersion"]!!

    repositories.applyStandard()
    repositories.applyGitHubPackages("base-types", rootProject)

    configurations.all {
        resolutionStrategy {
            force(
                io.spine.internal.dependency.Grpc.protobufPlugin
            )
        }
    }
}

subprojects {

    apply {
        plugin("kotlin")
        plugin(Dokka.pluginId)
    }

    LicenseReporter.generateReportIn(project)

    val coreVersion: String by extra

    dependencies {
        testImplementation("io.spine.tools:spine-testutil-server:$coreVersion")
        testImplementation(kotlin("test-junit5"))
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }

    tasks.test {
        useJUnitPlatform()

        testLogging {
            events = setOf(FAILED, SKIPPED)
            showExceptions = true
            showCauses = true
        }
    }

    val javaVersion = JavaVersion.VERSION_11.toString()
    kotlin {
        explicitApi()
        jvmToolchain {
            (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=" +
                        "kotlin.io.path.ExperimentalPathApi," +
                        "kotlin.ExperimentalUnsignedTypes," +
                        "kotlin.ExperimentalStdlibApi," +
                        "kotlin.experimental.ExperimentalTypeInference",
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

PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
JacocoConfig.applyTo(project)

val integrationTest by tasks.creating(RunBuild::class) {
    directory = "$rootDir/tests"
}

tasks["check"].finalizedBy(integrationTest)
