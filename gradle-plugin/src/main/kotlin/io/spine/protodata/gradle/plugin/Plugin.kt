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

package io.spine.protodata.gradle.plugin

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import io.spine.protodata.gradle.Artifacts
import io.spine.protodata.gradle.CleanTask
import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.LaunchTask
import io.spine.protodata.gradle.Names.EXTENSION_NAME
import io.spine.protodata.gradle.Names.PROTOC_PLUGIN
import io.spine.protodata.gradle.Names.USER_CLASSPATH_CONFIGURATION_NAME
import io.spine.tools.code.manifest.Version
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
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

    public companion object {

        /**
         * Reads the version of the plugin from the resources.
         */
        @JvmStatic
        @VisibleForTesting
        public fun readVersion(): String {
            val version = Version.fromManifestOf(Plugin::class.java).value
            return version
        }
    }
}

private fun Project.createLaunchTasks(extension: Extension, version: String) {
    val artifactConfig = configurations.create("protoDataRawArtifact") {
        it.isVisible = false
    }
    val cliDependency = Artifacts.fatCli(version)
    dependencies.add(artifactConfig.name, cliDependency)
    val userCpConfig = userClasspathConfiguration()
    sourceSets.forEach { sourceSet ->
        createLaunchTask(extension, sourceSet, artifactConfig, userCpConfig)
        createCleanTask(extension, sourceSet)
    }
}

private fun Project.userClasspathConfiguration() =
    configurations.create(USER_CLASSPATH_CONFIGURATION_NAME) {
        it.exclude(group = Artifacts.group, module = Artifacts.compiler)
    }

private fun Project.createExtension(): Extension {
    val extension = Extension(this)
    extensions.add(CodegenSettings::class.java, EXTENSION_NAME, extension)
    return extension
}

private fun Project.createLaunchTask(
    ext: Extension, sourceSet: SourceSet, artifactConfig: Configuration, userCpConfig: Configuration
) {
    val taskName = LaunchTask.nameFor(sourceSet)
    tasks.create<LaunchProtoData>(taskName) {
        renderers = ext.renderers
        plugins = ext.plugins
        optionProviders = ext.optionProviders
        requestFile = ext.requestFile(sourceSet)
        sources = ext.sourceDir(sourceSet)
        targets = ext.targetDir(sourceSet)
        protoDataConfig = artifactConfig
        userClasspathConfig = userCpConfig
        project.afterEvaluate {
            compileCommandLine()
        }

        onlyIf { requestFile.get().asFile.exists() }
        dependsOn(artifactConfig.buildDependencies, userCpConfig.buildDependencies)
        javaCompileFor(sourceSet)?.dependsOn(this)
        kotlinCompileFor(sourceSet)?.dependsOn(this)
    }
}

private fun Project.createCleanTask(ext: Extension, sourceSet: SourceSet) {
    val project = this
    val taskName = CleanTask.nameFor(sourceSet)
    tasks.create<Delete>(taskName) {
        delete(ext.targetDir(sourceSet))

        tasks.getByName("clean").dependsOn(this)
        val launchTask = LaunchTask.get(project, sourceSet)
        launchTask.mustRunAfter(this)
    }
}

private const val PROTOBUF_PLUGIN = "com.google.protobuf"

private fun Project.configureWithProtobufPlugin(extension: Extension, version: String) {
    if (pluginManager.hasPlugin(PROTOBUF_PLUGIN)) {
        configureProtobufPlugin(extension, version)
    } else {
        pluginManager.withPlugin(PROTOBUF_PLUGIN) {
            configureProtobufPlugin(extension, version)
        }
    }
}

/**
 * Verifies if the project has `java` plugin or `compileKotlin` or `compileTestKotlin` tasks.
 *
 * Even though current Protobuf support of Kotlin is based on Java codegen (and therefore
 * it's likely that Java would be enabled in the project in the near future for Kotlin proto
 * code to be generated), it may change someday. This method assumes such case when it checks
 * for Kotlin compilation tasks.
 */
private fun Project.hasJavaOrKotlin(): Boolean {
    if (pluginManager.hasPlugin("java")) {
        return true
    }
    val compileKotlin = tasks.findByName("compileKotlin")
    val compileTestKotlin = tasks.findByName("compileTestKotlin")
    return compileKotlin != null || compileTestKotlin != null
}

private fun Project.configureProtobufPlugin(extension: Extension, version: String) =
    protobuf {
        plugins {
            id(PROTOC_PLUGIN) {
                artifact = Artifacts.protocPlugin(version)
            }
        }
        generateProtoTasks {
            all().forEach { task ->
                if (hasJavaOrKotlin()) {
                    task.builtins.maybeCreate("kotlin")
                }
                val sourceSet = task.sourceSet
                task.plugins {
                    id(PROTOC_PLUGIN) {
                        val requestFile = extension.requestFile(sourceSet)
                        val path = requestFile.get().asFile.absolutePath
                        option(path.base64Encoded())
                    }
                }
                val launchTask = LaunchTask.get(project, sourceSet)
                launchTask.dependsOn(task)
            }
        }
        generatedFilesBaseDir = "$buildDir/generated-proto/"
    }

private fun Project.configureSourceSets(extension: Extension) {
    afterEvaluate {
        sourceSets.forEach(extension::configureSourceSet)
    }
}

private fun Extension.configureSourceSet(sourceSet: SourceSet) {
    val sourceDirs = sourceDir(sourceSet).getOrElse(listOf())
    val targetDirs = targetDir(sourceSet).get()

    sourceSet.java.srcDir(targetDirs)
    if (sourceDirs.isEmpty()) {
        return
    }
    val task = project.javaCompileFor(sourceSet)!!
    sourceDirs.asSequence()
        .zip(targetDirs.asSequence())
        .filter { it.first != it.second }
        .forEach { (sourceDir, _) ->
            task.source = task.source.filter { file -> !file.residesIn(sourceDir) }.asFileTree
        }
}

private fun File.residesIn(directory: Directory): Boolean =
    residesIn(directory.asFile)

private fun File.residesIn(directory: File): Boolean =
    canonicalFile.startsWith(directory.absolutePath)

private fun Project.configureIdea(extension: Extension) {
    afterEvaluate {
        val duplicateClassesDir = file(extension.srcBaseDir)
        pluginManager.withPlugin("idea") {
            val idea = extensions.getByType<IdeaModel>()
            with(idea.module) {
                sourceDirs = filterSources(sourceDirs, duplicateClassesDir)
                testSources.filter { !it.residesIn(duplicateClassesDir) }
                generatedSourceDirs = filterSources(generatedSourceDirs, duplicateClassesDir)
            }
        }
    }
}

private fun filterSources(sources: Set<File>, excludeDir: File): Set<File> =
    sources.filter { !it.residesIn(excludeDir) }.toSet()
