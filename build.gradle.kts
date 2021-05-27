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

import io.spine.gradle.internal.Truth
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Protobuf
import io.spine.internal.gradle.PublishingRepos
import io.spine.internal.gradle.Scripts
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.spinePublishing
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

    @Suppress("RemoveRedundantQualifierName")
    io.spine.internal.gradle.doApplyStandard(repositories)

    dependencies {
        classpath("io.spine.tools:spine-mc-java:2.0.0-SNAPSHOT.30")
        @Suppress("RemoveRedundantQualifierName")
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib)
    }
}

plugins {
    kotlin("jvm") version "1.5.0"
    id("org.jetbrains.dokka") version "1.4.32"
    idea
}

spinePublishing {
    targetRepositories.add(PublishingRepos.gitHub("ProtoData"))
    projectsToPublish.addAll(
        "cli",
        "compiler",
        "protoc"
    )
    spinePrefix.set(false)
}

allprojects {
    group = "io.spine.protodata"
    version = "0.0.10"

    repositories.applyStandard()
}

subprojects {

    apply {
        plugin("kotlin")
        plugin("idea")
        plugin("org.jetbrains.dokka")
        plugin("io.spine.mc-java")
        plugin(Protobuf.GradlePlugin.id)
        from(Scripts.modelCompiler(project))
    }

    val spineCoreVersion = "2.0.0-SNAPSHOT.23"

    dependencies {
        implementation("io.spine:spine-server:$spineCoreVersion")
        Protobuf.libs.forEach { implementation(it) }

        testImplementation(kotlin("test-junit5"))
        Truth.libs.forEach { implementation(it) }
        testImplementation("io.spine.tools:spine-testutil-server:$spineCoreVersion")
        testRuntimeOnly(JUnit.runner)
    }

    tasks.test {
        useJUnitPlatform()

        testLogging {
            events = setOf(PASSED, FAILED, SKIPPED)
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

    kotlin {
        explicitApi()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=kotlin.io.path.ExperimentalPathApi,kotlin.ExperimentalUnsignedTypes",
                "-Xinline-classes",
                "-Xjvm-default=all"
            )
        }
    }

    val dokkaJavadoc by tasks.getting(DokkaTask::class)

    tasks.register("javadocJar", Jar::class) {
        from(dokkaJavadoc.outputDirectory)
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
    }
}
