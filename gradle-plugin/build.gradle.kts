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

import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Kotlin
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.isSnapshot

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish").version("0.18.0")
    `version-to-resources`
    `write-manifest`
    `detekt-code-analysis`
    jacoco
}

val spine = Spine(project)

@Suppress(
    "UNUSED_VARIABLE" /* `test` and `functionalTest`*/,
    "UnstableApiUsage" /* testing suites feature */
)
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Kotlin.gradlePluginLib)
                implementation(gradleKotlinDsl())
                implementation(Protobuf.GradlePlugin.lib)
                implementation(spine.pluginBase)
                implementation(spine.pluginTestlib)
            }
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Kotlin.gradlePluginLib)
                implementation(Kotlin.testJUnit5)
                implementation(spine.pluginBase)
                implementation(spine.testlib)
                implementation(spine.pluginTestlib)
                implementation(project(":gradle-plugin"))
            }
        }
    }
}

val toolBaseVersion: String by extra

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly(Protobuf.GradlePlugin.lib)
    api(project(":gradle-api"))

    implementation(project(":cli-api"))
    implementation(spine.toolBase)
    implementation(spine.pluginBase)
    implementation(Kotlin.gradlePluginApi)
}

val testsDependOnProjects = listOf(
    "cli", "cli-api", "compiler", "protoc", "test-env", "gradle-api", "gradle-plugin"
)

tasks.withType<Test>().configureEach {
    val task = this
    testsDependOnProjects.forEach { project ->
        task.dependsOn(":$project:publishToMavenLocal")
    }
}

val pluginName = "protoDataPlugin"

gradlePlugin {
    plugins {
        create(pluginName) {
            id = "io.spine.protodata"
            implementationClass = "io.spine.protodata.gradle.plugin.Plugin"
            displayName = "ProtoData"
            description = "Sets up the ProtoData tool to be used in your project."
        }
    }
    val functionalTest by sourceSets.getting
    testSourceSets(
        functionalTest
    )
}

pluginBundle {
    website = "https://spine.io/"
    vcsUrl = "https://github.com/SpineEventEngine/ProtoData.git"
    tags = listOf("spine", "protobuf", "protodata", "code generation", "codegen")

    mavenCoordinates {
        groupId = "io.spine"
        artifactId = "protodata"
        version = project.version.toString()
    }

    plugins {
        named(pluginName) {
            version = project.version.toString()
        }
    }
}

val protoDataVersion: String by extra

val publishPlugins: Task by tasks.getting {
    enabled = !protoDataVersion.isSnapshot()
}

val publish: Task by tasks.getting {
    dependsOn(publishPlugins)
}

tasks {
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }

    ideaModule {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/13480")
    }

    publishPlugins {
        notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/21283")
    }
}

/**
 * Do it here because the call in `subprojects` does not have effect on the dependency
 * of the `publishPluginJar` on `createVersionFile`.
 */
configureTaskDependencies()
