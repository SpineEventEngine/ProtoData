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
import io.spine.internal.gradle.standardToSpineSdk

buildscript {
    standardSpineSdkRepositories()
}

plugins {
    java
    id("com.google.protobuf")
    id("@PROTODATA_PLUGIN_ID@") version "@PROTODATA_VERSION@"
}

repositories {
    mavenLocal() // Must come first for `protodata-test-env`.
    standardToSpineSdk()
}

dependencies {
    protoData("io.spine.protodata:protodata-test-env:+")
}

protobuf {
    protoc {
        artifact = io.spine.internal.dependency.Protobuf.compiler
    }
}

protoData {
    plugins("io.spine.protodata.test.TestPlugin", "io.spine.protodata.test.UnderscorePrefixPlugin")

    pathsFor("main") {
        source = "$buildDir/generated/source/proto/main/java"
        target = "$buildDir/foomain"
        language = "java"
    }

    pathsFor("test") {
        source = "$buildDir/generated-proto/test/kotlin"
        target = "$buildDir/footest"
        language = "java"
    }
}