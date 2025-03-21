/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import io.spine.dependency.build.Dokka
import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJava
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.dependency.test.Truth
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.IncrementGuard
import io.spine.gradle.report.license.LicenseReporter
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    `java-library`
    kotlin("jvm")
    id("net.ltgt.errorprone")
    id("detekt-code-analysis")
    id("dokka-for-java")
    id("dokka-for-kotlin")
    jacoco
    idea
}

apply {
    plugin(Dokka.GradlePlugin.id)
}

apply<IncrementGuard>()
LicenseReporter.generateReportIn(project)

/**
 * The alias for typed extensions functions related to modules of this project.
 */
typealias Module = Project

project.run {
    forceConfigurations()
    setDependencies()

    configureJava()
    configureKotlin()

    setupTests()
    configureDocTasks()

    applyGeneratedDirectories()
    configureTaskDependencies()
}

fun Module.setDependencies() {
    dependencies {
        ErrorProne.apply {
            errorprone(core)
        }
        testImplementation(CoreJava.testUtilServer)
        testImplementation(kotlin("test-junit5"))
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }
}

fun Module.forceConfigurations() {
    configurations.all {
        resolutionStrategy {
            force(
                Protobuf.compiler,
                Base.lib,
                ToolBase.lib,
                Validation.runtime,
            )
        }
    }
}

fun Module.setupTests() {
    tasks.test {
        useJUnitPlatform()

        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            showExceptions = true
            showCauses = true
        }
    }
}

/**
 * Adds directories with the generated source code to source sets of the project and
 * to IntelliJ IDEA module settings.
 */
fun Module.applyGeneratedDirectories() {

    /* The name of the root directory with the generated code. */
    val generatedDir = "${projectDir}/generated"

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

fun Module.configureJava() {
    java {
        toolchain.languageVersion.set(BuildSettings.javaVersion)
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

fun Module.configureKotlin() {
    kotlin {
        explicitApi()
        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            setFreeCompilerArgs()
        }
    }
}

fun Module.configureDocTasks() {
    val dokkaJavadoc by tasks.getting(DokkaTask::class)
    tasks.register("javadocJar", Jar::class) {
        from(dokkaJavadoc.outputDirectory)
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        configureForKotlin()
    }
}
