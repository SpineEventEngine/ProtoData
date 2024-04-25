/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import com.google.protobuf.gradle.id
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Spine
import org.gradle.api.file.DuplicatesStrategy.INCLUDE

plugins {
    protobuf
    `java-test-fixtures`
}

protobuf {
    protoc {
        artifact = io.spine.internal.dependency.Protobuf.compiler
    }

    val scriptPluginId = "codegen-request"
    plugins {
        create(scriptPluginId) {
            path = "$projectDir/store-request.sh"
        }
    }

    // Allows test suites to fetch generated Java files generated for the `testFixtures`
    // source set as resources.
    generateProtoTasks {
        ofSourceSet("testFixtures").forEach { task ->
            tasks.processTestResources {
                from(task.outputs)
                duplicatesStrategy = INCLUDE
            }
            task.plugins {
                id(scriptPluginId)
            }
        }
    }
}

dependencies {
    api(Spine.testlib)
    api(Spine.CoreJava.testUtilServer)
    api(project(":api"))
    api(project(":backend"))

    // For `google/type/` proto types used in stub domains.
    testFixturesImplementation(Grpc.api)
}
