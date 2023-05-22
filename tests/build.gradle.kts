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
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.standardToSpineSdk
import io.spine.internal.gradle.testing.configureLogging
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("RemoveRedundantQualifierName")
plugins {
    java
    kotlin("jvm") apply false
    idea
    protobuf
}

subprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("idea")
        plugin("com.google.protobuf")
        from("$rootDir/../version.gradle.kts")
    }

    repositories.standardToSpineSdk()

    val protoDataVersion: String by extra
    configurations {
        forceVersions()
        all {
            resolutionStrategy {
                force(
                    Spine.base,
                    Spine.toolBase,
                    Spine.validation.runtime,
                    Spine.logging,
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
//        generateProtoTasks.all().configureEach {
//            excludeProtocOutput()
//            setupKotlinCompile()
//        }
    }

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
        testRuntimeOnly(JUnit.runner)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        configureLogging()
    }
}
