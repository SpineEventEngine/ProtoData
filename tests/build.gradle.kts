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

import io.spine.dependency.lib.Caffeine
import io.spine.dependency.lib.Grpc
import io.spine.dependency.test.JUnit
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinX
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.test.Truth
import io.spine.dependency.local.Logging
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.Spine
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.Kotest
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.standardToSpineSdk
import io.spine.gradle.testing.configureLogging
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("RemoveRedundantQualifierName")
buildscript {
    dependencies {
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib)
        classpath(io.spine.dependency.lib.Kotlin.gradlePluginLib)
    }
}

plugins {
    java
    kotlin("jvm") apply false
    id("com.google.protobuf")
    idea
}

subprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("idea")
        plugin("com.google.protobuf")
        from("$rootDir/../version.gradle.kts")
    }

    val protoDataVersion: String by extra
    group = "io.spine.protodata.tests"
    version = protoDataVersion

    repositories.standardToSpineSdk()

    configurations {
        forceVersions()
        all {
            exclude(group = "io.spine", module = "spine-flogger-api")
            exclude(group = "io.spine", module = "spine-logging-backend")

            resolutionStrategy {
                @Suppress("DEPRECATION") // To force `Kotlin.stdLibJdk7`.
                force(
                    Kotlin.stdLibJdk7,
                    KotlinX.Coroutines.core,
                    KotlinX.Coroutines.test,
                    KotlinX.Coroutines.jdk8,
                    Caffeine.lib,
                    Grpc.api,
                    Spine.base,
                    ToolBase.lib,
                    Validation.runtime,
                    Logging.lib,
                    Logging.libJvm,
                    Logging.middleware,
                    Spine.reflect,
                    ProtoData.backend,
                    ProtoData.java,
                    Jackson.Junior.objects
                )
            }
        }
    }

    protobuf {
        protoc {
            artifact = Protobuf.compiler
        }
    }

    disableDocumentationTasks()

    tasks.withType<KotlinCompile> {
        setFreeCompilerArgs()
    }

    val generatedFiles = "$projectDir/generated"
    tasks.getByName<Delete>("clean") {
        delete.add(generatedFiles)
    }

    dependencies {
        Protobuf.libs.forEach { implementation(it) }

        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testImplementation(Kotest.assertions)
        testRuntimeOnly(JUnit.runner)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        configureLogging()
    }
}
