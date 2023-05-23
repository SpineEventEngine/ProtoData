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
}

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
                implementation(Spine.pluginBase)
                implementation(Spine.pluginTestlib)
            }
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(JUnit.version)
            dependencies {
                implementation(Kotlin.gradlePluginLib)
                implementation(Kotlin.testJUnit5)
                implementation(Spine.pluginBase)
                implementation(Spine.testlib)
                implementation(Spine.pluginTestlib)
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

    implementation(project(":api"))
    implementation(project(":cli-api"))
    implementation(Spine.toolBase)
    implementation(Spine.pluginBase)
    implementation(Kotlin.gradlePluginApi)
}

/**
 * Make functional tests depend on publishing all the submodules to Maven Local so that
 * the gradle plugin can get all the dependencies when it's applied to the test projects.
 */
val functionalTest: Task by tasks.getting {
    val task = this
    rootProject.subprojects.forEach { subproject ->
        task.dependsOn(":${subproject.name}:publishToMavenLocal")
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

    val pubVersion = project.version.toString()
    mavenCoordinates {
        groupId = "io.spine"
        artifactId = "protodata"
        version = pubVersion
    }

    plugins {
        named(pluginName) {
            version = pubVersion
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
