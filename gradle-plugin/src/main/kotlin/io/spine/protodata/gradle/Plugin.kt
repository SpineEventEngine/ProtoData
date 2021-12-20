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

package io.spine.protodata.gradle

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.api.Plugin as GradlePlugin

/**
 * The ProtoData Gradle plugin.
 *
 * Adds the `launchProtoData` task which runs the executable with the arguments assembled from
 * the configuration of this plugin.
 *
 * The users can submit configuration parameters, such as renderer and plugin class names, etc. via
 * the `protoData { }` extension.
 *
 * The users can submit the user classpath to the ProtoData by declaring dependencies using
 * the `protoData` configuration.
 *
 * Example:
 * ```
 * protoData {
 *     renderers("com.acme.MyRenderer")
 *     plugins("com.acme.MyPlugin")
 * }
 *
 * dependencies {
 *     protoData(project(":my-plugin"))
 * }
 * ```
 */
public class Plugin : GradlePlugin<Project> {

    override fun apply(target: Project) {
        val version = readVersion()
        with(target) {
            val extension = createExtension()
            configureWithProtobufPlugin(extension, version)
            createLaunchTasks(extension, version)
            configureSourceSets(extension)
            configureIdea(extension)
        }
    }

    private fun readVersion(): String {
        val resource = Plugin::class.java.classLoader.getResource(VERSION_RESOURCE)!!
        return resource.readText()
    }
}

/**
 * The resource file containing the version of ProtoData.
 *
 * Such a resource name might be duplicated in other places in ProtoData code base.
 * The reason for this is to avoid creating an extra dependency for the Gradle plugin,
 * so that the users wouldn't have to declare a custom Maven repository to use the plugin.
 */
private const val VERSION_RESOURCE = "version.txt"

private const val PROTOC_PLUGIN = "protodata"

private fun Project.createLaunchTasks(extension: Extension, version: String) {
    val artifactConfig = configurations.create("protoDataRawArtifact") {
        it.isVisible = false
    }
    dependencies.add(artifactConfig.name, "io.spine.protodata:cli:$version")
    val userCpConfig = configurations.create("protoData")
    sourceSets.forEach { sourceSet ->
        createLaunchTask(extension, sourceSet, artifactConfig, userCpConfig)
        createCleanTask(extension, sourceSet)
    }
}

private fun Project.createExtension(): Extension {
    val extension = Extension(this)
    extensions.add("protoData", extension)
    return extension
}

private fun Project.createLaunchTask(
    ext: Extension, sourceSet: SourceSet, artifactConfig: Configuration, userCpConfig: Configuration
) {
    val taskName = launchTaskName(sourceSet)
    tasks.create<LaunchProtoData>(taskName) {
        renderers = ext.renderers
        plugins = ext.plugins
        optionProviders = ext.optionProviders
        options = ext.options
        requestFile = ext.requestFile(sourceSet)
        source = ext.sourceDir(sourceSet)
        target = ext.targetDir(sourceSet)
        protoDataConfig = artifactConfig
        userClasspathConfig = userCpConfig
        project.afterEvaluate {
            compileCommandLine()
        }

        onlyIf { requestFile.get().asFile.exists() }
        dependsOn(artifactConfig.buildDependencies, userCpConfig.buildDependencies)
        javaCompileFor(sourceSet)?.dependsOn(this)
    }
}

private fun Project.createCleanTask(ext: Extension, sourceSet: SourceSet) {
    val taskName = cleanTaskName(sourceSet)
    tasks.create<Delete>(taskName) {
        delete(ext.targetDir(sourceSet))

        tasks.getByName("clean").dependsOn(this)
        tasks.getByName(launchTaskName(sourceSet)).mustRunAfter(this)
    }
}

private fun Project.configureWithProtobufPlugin(extension: Extension, version: String) {
    if (pluginManager.hasPlugin("com.google.protobuf")) {
        configureProtobufPlugin(extension, version)
    } else {
        pluginManager.withPlugin("com.google.protobuf") {
            configureProtobufPlugin(extension, version)
        }
    }
}

private fun Project.configureProtobufPlugin(extension: Extension, version: String) =
    protobuf {
        plugins {
            id(PROTOC_PLUGIN) {
                artifact = "io.spine.protodata:protoc:$version:exe@jar"
            }
        }
        generateProtoTasks {
            all().forEach {
                it.plugins {
                    id(PROTOC_PLUGIN) {
                        val requestFile = extension.requestFile(it.sourceSet)
                        val path = requestFile.get().asFile.absolutePath
                        option(path.base64Encoded())
                    }
                }
                project.tasks.getByName(launchTaskName(it.sourceSet)).dependsOn(it)
            }
        }
        generatedFilesBaseDir = "$buildDir/generated-proto/"
    }

private fun launchTaskName(sourceSet: SourceSet): String =
    "launchProtoData${sourceSet.capitalizedName}"

private fun cleanTaskName(sourceSet: SourceSet): String =
    "cleanProtoData${sourceSet.capitalizedName}"

private val SourceSet.capitalizedName: String
    get() = name.replaceFirstChar { it.uppercase() }

private fun Project.configureSourceSets(extension: Extension) {
    afterEvaluate {
        sourceSets.forEach { sourceSet ->
            val sourceDir = file(extension.sourceDir(sourceSet))
            val targetDir = file(extension.targetDir(sourceSet))

            sourceSet.java.srcDir(targetDir)
            if (sourceDir != targetDir) {
                val task = javaCompileFor(sourceSet)!!
                task.source = task.source.filter { file -> !file.residesIn(sourceDir) }.asFileTree
            }
        }
    }
}

private fun File.residesIn(directory: File): Boolean =
    canonicalFile.startsWith(directory.canonicalFile)

private fun Project.configureIdea(extension: Extension) {
    afterEvaluate {
        val duplicateClassesDir = file(extension.srcBaseDir)
        pluginManager.withPlugin("idea") {
            val idea = extensions.getByType<IdeaModel>()
            with(idea.module) {
                sourceDirs = filterSources(sourceDirs, duplicateClassesDir)
                testSourceDirs = filterSources(testSourceDirs, duplicateClassesDir)
                generatedSourceDirs = filterSources(generatedSourceDirs, duplicateClassesDir)
            }
        }
    }
}

private fun filterSources(sources: Set<File>, excludeDir: File): Set<File> =
    sources.filter { !it.residesIn(excludeDir) }.toSet()
