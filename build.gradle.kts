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

import Build_gradle.Subproject
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.RunBuild
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.SpinePublishing
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.standardToSpineSdk
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    standardSpineSdkRepositories()
    dependencies {
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib)
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
    }
}

plugins {
    kotlin("jvm")
    errorprone
    idea
    jacoco
    `gradle-doctor`
}

spinePublishing {
    modules = setOf(
        "cli",
        "compiler",
        "protoc",
        "codegen-java",
        "gradle-api",
        "test-env"
    )
    destinations = setOf(
        PublishingRepos.gitHub("ProtoData"),
        PublishingRepos.cloudArtifactRegistry
    )
    artifactPrefix = "protodata-"
}

val spine = Spine(project)

allprojects {
    apply {
        from("$rootDir/version.gradle.kts")
        plugin("idea")
        plugin("project-report")
    }

    group = "io.spine.protodata"
    version = extra["protoDataVersion"]!!

    repositories.standardToSpineSdk()

    configurations.all {
        resolutionStrategy {
            force(
                io.spine.internal.dependency.Grpc.protobufPlugin,
                spine.base,
                spine.testlib,
                spine.server
            )
        }
    }
}

object BuildSettings {
    const val javaVersion = 11

    /**
     * Temporarily use this version, since 3.21.x is known to provide
     * a broken `protoc-gen-js` artifact and Kotlin code without access modifiers.
     *
     * See https://github.com/protocolbuffers/protobuf-javascript/issues/127.
     *     https://github.com/protocolbuffers/protobuf/issues/10593
     */
    const val protocArtifact = "com.google.protobuf:protoc:3.19.6"
}

subprojects {
    applyPlugins()
    setDependencies()
    forceConfigurations()

    val javaVersion = JavaLanguageVersion.of(BuildSettings.javaVersion)

    configureJava(javaVersion)
    configureKotlin(javaVersion)
    setupTests()
    configureJavadoc()

    configureProtoc(BuildSettings.protocArtifact)

    val generated = "$projectDir/generated"
    applyGeneratedDirectories(generated)
    configureTaskDependencies()
}

PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
JacocoConfig.applyTo(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val projectsToPublish: Set<String> = the<SpinePublishing>().modules
val localPublish by tasks.registering {
    /*
       Integration tests need the plugin subproject published to Maven Local too
       because they apply the plugin.

       The plugin subproject is not added to the list of `projectsToPublish` because
       it is published from inside its `build.gradle.kts`.
     */
    val includingPlugin = projectsToPublish + "gradle-plugin"
    val pubTasks = includingPlugin.map { p ->
        val subProject = project(p)
        subProject.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}

val integrationTest by tasks.creating(RunBuild::class) {
    directory = "$rootDir/tests"
    dependsOn(localPublish)
}

tasks["check"].finalizedBy(integrationTest)

/**
 * The alias for typed extensions functions related to subprojects.
 */
typealias Subproject = Project

fun Subproject.applyPlugins() {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("net.ltgt.errorprone")
        plugin(Dokka.GradlePlugin.id)
        plugin(Protobuf.GradlePlugin.id)
    }
    LicenseReporter.generateReportIn(project)
}

fun Subproject.setDependencies() {
    val spine = Spine(project)
    dependencies {
        ErrorProne.apply {
            errorprone(core)
        }
        testImplementation(spine.coreJava.testUtilServer)
        testImplementation(kotlin("test-junit5"))
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }
}

fun Subproject.forceConfigurations() {
    configurations.all {
        resolutionStrategy {
            force(
                Protobuf.compiler,
            )
        }
    }
}

fun Subproject.setupTests() {
    tasks.test {
        useJUnitPlatform()

        testLogging {
            events = setOf(PASSED, FAILED, SKIPPED)
            showExceptions = true
            showCauses = true
        }
    }
}

/**
 * Adds directories with the generated source code to source sets of the project and
 * to IntelliJ IDEA module settings.
 *
 * @param generatedDir
 *          the name of the root directory with the generated code
 */
fun Subproject.applyGeneratedDirectories(generatedDir: String) {
    val generatedMain = "$generatedDir/main"
    val generatedJava = "$generatedMain/java"
    val generatedKotlin = "$generatedMain/kotlin"
    val generatedGrpc = "$generatedMain/grpc"
    val generatedSpine = "$generatedMain/spine"

    val generatedTest = "$generatedDir/test"
    val generatedTestJava = "$generatedTest/java"
    val generatedTestKotlin = "$generatedTest/kotlin"
    val generatedTestGrpc = "$generatedTest/grpc"
    val generatedTestSpine = "$generatedTest/spine"

    sourceSets {
        main {
            java.srcDirs(
                generatedJava,
                generatedGrpc,
                generatedSpine,
            )
            kotlin.srcDirs(
                generatedKotlin,
            )
        }
        test {
            java.srcDirs(
                generatedTestJava,
                generatedTestGrpc,
                generatedTestSpine,
            )
            kotlin.srcDirs(
                generatedTestKotlin,
            )
        }
    }

    idea {
        module {
            generatedSourceDirs.addAll(files(
                generatedJava,
                generatedKotlin,
                generatedGrpc,
                generatedSpine,
            ))
            testSources.from(
                generatedTestJava,
                generatedTestKotlin,
                generatedTestGrpc,
                generatedTestSpine,
            )
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
}

fun Project.configureJava(javaVersion: JavaLanguageVersion) {
    java {
        toolchain.languageVersion.set(javaVersion)
    }
    tasks {
        withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
            // https://stackoverflow.com/questions/38298695/gradle-disable-all-incremental-compilation-and-parallel-builds
            options.isIncremental = false
        }
        withType<org.gradle.jvm.tasks.Jar>().configureEach {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

fun Project.configureKotlin(javaVersion: JavaLanguageVersion) {
    kotlin {
        explicitApi()
        applyJvmToolchain(javaVersion.asInt())
    }

    tasks.withType<KotlinCompile> {
        setFreeCompilerArgs()
        // https://stackoverflow.com/questions/38298695/gradle-disable-all-incremental-compilation-and-parallel-builds
        incremental = false
    }
}

fun Project.configureJavadoc() {
    val dokkaJavadoc by tasks.getting(DokkaTask::class)
    tasks.register("javadocJar", Jar::class) {
        from(dokkaJavadoc.outputDirectory)
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
    }
}

fun Subproject.configureProtoc(protocArtifact: String) {
    project.protobuf {
        protoc {
            // Temporarily use this version, since 3.21.x is known to provide
            // a broken `protoc-gen-js` artifact.
            // See https://github.com/protocolbuffers/protobuf-javascript/issues/127.
            //
            // Once it is addressed, this artifact should be `Protobuf.compiler`.
            //
            // Also, this fixes the explicit API more for the generated Kotlin code.
            //
            artifact = protocArtifact
        }
    }
}
