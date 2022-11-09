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

import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Truth
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.Grpc
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.testing.configureLogging

@Suppress("RemoveRedundantQualifierName")
plugins {
    java
    idea
    protobuf
}

subprojects {
    apply {
        plugin("java")
        plugin("idea")
        plugin("com.google.protobuf")
        from("$rootDir/../version.gradle.kts")
    }

    repositories.applyStandard()
    repositories.applyGitHubPackages("base-types", rootProject)

    val protoDataVersion: String by extra
    val spine = io.spine.internal.dependency.Spine(project)
    configurations {
        forceVersions()
        all {
            resolutionStrategy {
                force(
                    io.spine.internal.dependency.Grpc.protobufPlugin,
                    spine.base,
                    spine.validation.runtime,
                    "io.spine.protodata:compiler:$protoDataVersion",
                    "io.spine.protodata:codegen-java:$protoDataVersion"
                )
            }
        }
    }

    protobuf {
        protoc {
            artifact = Protobuf.compiler
        }
    }

    val generatedFiles = "$projectDir/generated"
    tasks.getByName<Delete>("clean") {
        delete.add(generatedFiles)
    }

    dependencies {
        Protobuf.libs.forEach { implementation(it) }

        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        configureLogging()
    }
}
