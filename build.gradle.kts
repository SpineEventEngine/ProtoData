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

import io.spine.gradle.internal.Deps
import io.spine.gradle.internal.PublishingRepos
import io.spine.gradle.internal.spinePublishing
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    id("org.jetbrains.dokka") version "1.4.30"
    id("io.spine.tools.gradle.bootstrap").version("1.7.0")
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
    version = "0.0.6"
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "idea")
    apply(plugin = "io.spine.tools.gradle.bootstrap")
    apply(plugin = "org.jetbrains.dokka")

    spine.enableJava().server()

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        testImplementation(kotlin("test-junit5"))
        testRuntimeOnly(Deps.test.junit.runner)
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
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
        }
    }

    val dokkaJavadoc by tasks.getting(DokkaTask::class)

    tasks.register("javadocJar", Jar::class) {
        from(dokkaJavadoc.outputDirectory)
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
    }
}
