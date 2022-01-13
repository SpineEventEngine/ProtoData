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

import org.gradle.api.artifacts.dsl.RepositoryHandler

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)
}

plugins {
    java
    id("com.google.protobuf")
    //TODO:2022-01-13:alexander.yevsyukov: Replace version with a tag to be replaces on `ProjectSetup`.
    id("io.spine.proto-data") version "0.1.6"
}

fun RepositoryHandler.addCouple(baseUrl: String) {
    maven { url = uri("$baseUrl/releases") }
    maven { url = uri("$baseUrl/snapshots") }
}

repositories {
    mavenLocal()
    mavenCentral()

    addCouple("https://spine.mycloudrepo.io/public/repositories")
    addCouple("https://europe-maven.pkg.dev/spine-event-engine")
}

protoData {
    renderers("io.spine.protodata.test.TestRenderer")
    plugins("io.spine.protodata.test.TestPlugin")
}

dependencies {
    protoData("io.spine.protodata:testutil:+")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:+"
    }
}
