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

import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.lib.Caffeine
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJava
import io.spine.dependency.local.Logging
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.dependency.test.Kotest
import io.spine.dependency.test.Truth
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.testing.configureLogging

@Suppress("RemoveRedundantQualifierName")
buildscript {
    dependencies {
        classpath(io.spine.dependency.lib.Protobuf.GradlePlugin.lib)
        classpath(io.spine.dependency.lib.Kotlin.gradlePluginLib)
    }
}

plugins {
    java
    `kotlin-dsl`
    kotlin("jvm") apply false
    id("com.google.protobuf")
    idea
}
apply<BomsPlugin>()

repositories.standardToSpineSdk()

subprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("idea")
        plugin("com.google.protobuf")
        plugin("module-testing")
        from("$rootDir/../version.gradle.kts")
    }
    apply<BomsPlugin>()

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
                    KotlinPoet.lib,
                    Caffeine.lib,
                    Grpc.api,
                    Base.lib,
                    ToolBase.lib,
                    ToolBase.psiJava,
                    Validation.runtime,
                    Logging.lib,
                    Logging.libJvm,
                    Logging.middleware,
                    Reflect.lib,
                    ProtoData.api,
                    ProtoData.backend,
                    ProtoData.params,
                    CoreJava.server,
                    ProtoData.java,
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

    kotlin {
        compilerOptions {
            jvmTarget.set(BuildSettings.jvmTarget)
            setFreeCompilerArgs()
        }
    }

    val generatedFiles = "$projectDir/generated"
    tasks.getByName<Delete>("clean") {
        delete.add(generatedFiles)
    }

    dependencies {
        Protobuf.libs.forEach { implementation(it) }
    }
}
