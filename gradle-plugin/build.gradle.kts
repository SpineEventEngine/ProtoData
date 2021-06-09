/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import io.spine.internal.dependency.Protobuf
import io.spine.internal.gradle.Publish

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish").version("0.15.0")
    `version-to-resources`
}

val spineBaseVersion: String by extra

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(Protobuf.GradlePlugin.lib)

    testImplementation("io.spine.tools:spine-plugin-base:$spineBaseVersion")
    testImplementation("io.spine.tools:spine-plugin-testlib:$spineBaseVersion")
}

tasks.withType<Test> {
    dependsOn(":cli:publishToMavenLocal")
}

gradlePlugin {
    plugins {
        create("protoDataPlugin") {
            id = "io.spine.proto-data"
            implementationClass = "io.spine.protodata.gradle.Plugin"
            displayName = "Spine Bootstrap"
            description = "Prepares a Gradle project for development on Spine."
        }
    }
}

pluginBundle {
    website = "https://spine.io/"
    vcsUrl = "https://github.com/SpineEventEngine/ProtoData.git"
    tags = listOf("spine", "protobuf", "protodata", "code generation")

    mavenCoordinates {
        groupId = "io.spine"
        artifactId = "proto-data"
        version = project.version.toString()
    }

    plugins {
        named("protoDataPlugin") {
            version = project.version.toString()
        }
    }
}

tasks.create(Publish.taskName) {
    dependsOn("publishPlugins")
}
