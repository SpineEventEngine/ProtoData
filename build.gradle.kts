/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.RunBuild
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.SpinePublishing
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.standardToSpineSdk
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

buildscript {
    standardSpineSdkRepositories()
    val baseForBuildScript = io.spine.internal.dependency.Spine.baseForBuildScript
    dependencies {
        classpath(io.spine.internal.dependency.Protobuf.GradlePlugin.lib)
        classpath(baseForBuildScript)
        classpath(mcJava.pluginLib) {
            excludeSpineBase()
        }
    }
    configurations.all {
        resolutionStrategy {
            force(baseForBuildScript)
        }
    }
}

plugins {
    idea
    jacoco
    `gradle-doctor`
    `project-report`
}

/**
 * Publish all the modules, but `gradle-plugin`, which is published separately by its own.
 */
spinePublishing {
    modules = productionModules
        .map { project -> project.name }
        .toSet()
        .minus("gradle-plugin") // because of custom publishing.

    destinations = setOf(
        PublishingRepos.gitHub("ProtoData"),
        PublishingRepos.cloudArtifactRegistry
    )
    artifactPrefix = "protodata-"
}

allprojects {
    apply(plugin = Dokka.GradlePlugin.id)
    apply(from = "$rootDir/version.gradle.kts")
    group = "io.spine.protodata"
    version = extra["protoDataVersion"]!!

    repositories.standardToSpineSdk()

    configurations.all {
        resolutionStrategy {
            force(
                Grpc.ProtocPlugin.artifact,
                Spine.reflect,
                Spine.base,
                Spine.testlib,
                Spine.server
            )
        }
    }
}

subprojects {
    apply {
        plugin("module")
    }
}

PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)
JacocoConfig.applyTo(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val projectsToPublish: Set<String> = the<SpinePublishing>().modules
val localPublish by tasks.registering {
    /*
       Integration tests need the plugin subproject published to Maven Local too
       because they apply the plugin.

       The plugin subproject is not added to the list of `projectsToPublish` because
       it is published from inside its `build.gradle.kts`.
     */
    val includingPlugin = projectsToPublish + "gradle-plugin"
    val pubTasks = includingPlugin.map { p ->
        val subProject = project(p)
        subProject.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}

/**
 * The `integrationTest` task runs a Gradle build in the project located
 * under the `tests` subdirectory.
 *
 * This build should run _only_ if all tests of all modules passed.
 * Otherwise, integration tests make little sense.
 */
val integrationTest by tasks.creating(RunBuild::class) {
    directory = "$rootDir/tests"
    dependsOn(localPublish)
    subprojects.forEach {
        it.tasks.findByName("test")?.let { testTask ->
            this@creating.dependsOn(testTask)
        }
    }
}

/**
 * The `check` task is done if `integrationTest` passes.
 */
tasks["check"].dependsOn(integrationTest)

val dokkaHtmlMultiModule by tasks.getting(DokkaMultiModuleTask::class) {
    configureStyle()
}
