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

// Performs many Gradle configuration routines via extension functions.
@file:Suppress("TooManyFunctions")

package io.spine.protodata.gradle.plugin

import com.google.common.annotations.VisibleForTesting
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.gradle.GenerateProtoTask
import io.spine.protodata.gradle.Artifacts
import io.spine.protodata.gradle.CleanTask
import io.spine.protodata.gradle.CodegenSettings
import io.spine.protodata.gradle.LaunchTask
import io.spine.protodata.gradle.Names.EXTENSION_NAME
import io.spine.protodata.gradle.Names.PROTODATA_PROTOC_PLUGIN
import io.spine.protodata.gradle.ProtocPluginArtifact
import io.spine.tools.code.manifest.Version
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.gradle.protobuf.generatedSourceProtoDir
import io.spine.tools.gradle.protobuf.protobufExtension
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.SourceDirectorySet
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
            val ext = createExtension()
            createConfigurations(version)
            createTasks(ext)
            configureWithProtobufPlugin(version, ext)
            configureIdea()
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

private const val PROTO_DATA_RAW_ARTIFACT = "protoDataRawArtifact"

/**
 * The name of the Gradle Configuration created by ProtoData Gradle plugin for holding
 * user-defined classpath.
 */
public const val USER_CLASSPATH_CONFIGURATION_NAME: String = "protoData"

private fun Project.createExtension(): Extension {
    val extension = Extension(this)
    extensions.add(CodegenSettings::class.java, EXTENSION_NAME, extension)
    return extension
}

/**
 * Creates configurations for `protoDataRawArtifact` and user-defined classpath,
 * and adds dependency on [Artifacts.fatCli].
 */
private fun Project.createConfigurations(protoDataVersion: String) {
    val artifactConfig = configurations.create(PROTO_DATA_RAW_ARTIFACT) {
        it.isVisible = false
    }
    val cliDependency = Artifacts.fatCli(protoDataVersion)
    dependencies.add(artifactConfig.name, cliDependency)

    configurations.create(USER_CLASSPATH_CONFIGURATION_NAME) {
        it.exclude(group = Artifacts.group, module = Artifacts.compiler)
    }
}

private val Project.protoDataRawArtifact: Configuration
    get() = configurations.getByName(PROTO_DATA_RAW_ARTIFACT)

private val Project.userClasspath: Configuration
    get() = configurations.getByName(USER_CLASSPATH_CONFIGURATION_NAME)

/**
 * Creates [LaunchProtoData] and `clean` task for all source sets of this project
 * available by the time of the call.
 *
 * There may be cases of source sets added by other plugins after this method is invoked.
 * Such situations are handled by [Project.handleLaunchTaskDependency] invoked by
 * [Project.configureProtobufPlugin].
 *
 * @see [Project.handleLaunchTaskDependency]
 * @see [Project.configureProtobufPlugin]
 */
private fun Project.createTasks(ext: Extension) {
    sourceSets.forEach { sourceSet ->
        createLaunchTask(sourceSet, ext)
        createCleanTask(sourceSet, ext)
    }
}

/**
 * Creates [LaunchProtoData] to serve the given [sourceSet].
 */
@CanIgnoreReturnValue
private fun Project.createLaunchTask(sourceSet: SourceSet, ext: Extension): LaunchProtoData {
    val taskName = LaunchTask.nameFor(sourceSet)
    val result = tasks.create<LaunchProtoData>(taskName) {
        renderers = ext.renderers
        plugins = ext.plugins
        optionProviders = ext.optionProviders
        requestFile = ext.requestFile(sourceSet)
        protoDataConfig = protoDataRawArtifact
        userClasspathConfig = userClasspath
        project.afterEvaluate {
            sources = ext.sourceDirs(sourceSet)
            targets = ext.targetDirs(sourceSet)
            compileCommandLine()
        }
        setPreLaunchCleanup()
        onlyIf {
            checkRequestFile(sourceSet)
        }
        dependsOn(
            protoDataRawArtifact.buildDependencies,
            userClasspath.buildDependencies
        )
        val launchTask = this
        javaCompileFor(sourceSet)?.dependsOn(launchTask)
        kotlinCompileFor(sourceSet)?.dependsOn(launchTask)
    }
    return result
}

private fun Project.createCleanTask(sourceSet: SourceSet, ext: Extension) {
    val project = this
    val cleanSourceSet = CleanTask.nameFor(sourceSet)
    tasks.create<Delete>(cleanSourceSet) {
        delete(ext.targetDirs(sourceSet))

        tasks.getByName("clean").dependsOn(this)
        val launchTask = LaunchTask.get(project, sourceSet)
        launchTask.mustRunAfter(this)
    }
}

private const val PROTOBUF_PLUGIN = "com.google.protobuf"

private fun Project.configureWithProtobufPlugin(protoDataVersion: String, ext: Extension) {
    val protocArtifact = ProtocPluginArtifact(protoDataVersion)
    if (pluginManager.hasPlugin(PROTOBUF_PLUGIN)) {
        configureProtobufPlugin(protocArtifact, ext)
    } else {
        pluginManager.withPlugin(PROTOBUF_PLUGIN) {
            configureProtobufPlugin(protocArtifact, ext)
        }
    }
}

/**
 * Verifies if the project has `java` plugin or `compileKotlin` or `compileTestKotlin` tasks.
 *
 * The current Protobuf support of Kotlin is based on Java codegen. Therefore,
 * it's likely that Java would be enabled in the project for Kotlin proto
 * code to be generated. Though, it may change someday and Kotlin support of Protobuf would be
 * self-sufficient. This method assumes such case when it checks the presence of
 * Kotlin compilation tasks.
 */
private fun Project.hasJavaOrKotlin(): Boolean {
    if (pluginManager.hasPlugin("java")) {
        return true
    }
    val compileKotlin = tasks.findByName("compileKotlin")
    val compileTestKotlin = tasks.findByName("compileTestKotlin")
    return compileKotlin != null || compileTestKotlin != null
}

private fun Project.configureProtobufPlugin(
    protocPlugin: ProtocPluginArtifact,
    ext: Extension
) {
    protobufExtension?.apply {
        plugins {
            it.create(PROTODATA_PROTOC_PLUGIN) { locator ->
                locator.artifact = protocPlugin.coordinates
            }
        }

        // The below block adds a configuration action for the `GenerateProtoTaskCollection`.
        // We cannot do it like `generateProtoTasks.all().forEach { ... }` because it breaks the
        // order of the configuration of the `GenerateProtoTaskCollection`. This, in turn,
        // leads to missing generated sources in the `compileJava` task.
        generateProtoTasks {
            it.all().forEach { task ->
                configureProtoTask(task, ext)
            }
        }
    }
}

private fun Project.configureProtoTask(task: GenerateProtoTask, ext: Extension) {
    if (hasJavaOrKotlin()) {
        task.builtins.maybeCreate("kotlin")
    }
    val sourceSet = task.sourceSet
    task.plugins.apply {
        create(PROTODATA_PROTOC_PLUGIN) {
            val requestFile = ext.requestFile(sourceSet)
            val path = requestFile.get().asFile.absolutePath
            val nameEncoded = path.base64Encoded()
            it.option(nameEncoded)
            if (logger.isDebugEnabled) {
                logger.debug("The task `${task.name}` got plugin `$PROTODATA_PROTOC_PLUGIN`" +
                        " with the option `$nameEncoded`.")
            }
        }
    }
    task.excludeProtocOutput()
    handleLaunchTaskDependency(task, sourceSet, ext)
}

/**
 * Exclude [GenerateProtoTask.outputBaseDir] from Java source set directories to avoid
 * duplicated source code files.
 */
private fun GenerateProtoTask.excludeProtocOutput() {
    val protocOutputDir = File(outputBaseDir).parentFile
    val java: SourceDirectorySet = sourceSet.java

    // Filter out directories belonging to `build/generated/source/proto`.
    val newSourceDirectories = java.sourceDirectories
        .filter { !it.residesIn(protocOutputDir) }
        .toSet()

    // Clear the source directories of the Java source set.
    // This trick was needed when building `base` module of Spine.
    // Otherwise, the `java` plugin would complain about duplicate source files.
    java.setSrcDirs(listOf<String>())

    // Add the filtered directories back to the Java source set.
    java.srcDirs(newSourceDirectories)

    // Add copied files to the Java source set.
    java.srcDir(generatedDir("java"))
    java.srcDir(generatedDir("kotlin"))
}

/**
 * Obtains the `generated` directory for the source set of the task.
 *
 * If [language] is specified returns the subdirectory for this language.
 */
private fun GenerateProtoTask.generatedDir(language: String = ""): File {
    val path = "${project.targetBaseDir}/${sourceSet.name}/$language"
    return File(path)
}

/**
 * Obtains the name of the directory where ProtoData places generated files.
 */
private val Project.targetBaseDir: String
    get() {
        val ext = extensions.getByType(CodegenSettings::class.java)
        return ext.targetBaseDir.toString()
    }

/**
 * Makes a [LaunchProtoData], if it exists for the given [sourceSet], depend on
 * the given [GenerateProtoTask].
 *
 * If the [LaunchProtoData] task does not exist (which may be the case for custom source sets
 * created by other plugins), arranges the task creation on [Project.afterEvaluate].
 * In this case the [CleanTask] is also created with appropriate dependencies.
 */
private fun Project.handleLaunchTaskDependency(
    task: GenerateProtoTask,
    sourceSet: SourceSet,
    ext: Extension
) {
    var launchTask: Task? = LaunchTask.find(project, sourceSet)
    if (launchTask != null) {
        launchTask.dependsOn(task)
    } else {
        project.afterEvaluate {
            launchTask = createLaunchTask(sourceSet, ext)
            launchTask!!.dependsOn(task)
            createCleanTask(sourceSet, ext)
        }
    }
}

private fun Project.configureIdea() {
    afterEvaluate {
        val protocOutput = file(generatedSourceProtoDir)
        pluginManager.withPlugin("idea") {
            val idea = extensions.getByType<IdeaModel>()
            with(idea.module) {
                sourceDirs = filterSources(sourceDirs, protocOutput)
                testSources.filter { !it.residesIn(protocOutput) }
                generatedSourceDirs = filterSources(generatedSourceDirs, protocOutput)
            }
        }
    }
}

private fun filterSources(sources: Set<File>, excludeDir: File): Set<File> =
    sources.filter { !it.residesIn(excludeDir) }.toSet()

private fun File.residesIn(directory: File): Boolean =
    canonicalFile.startsWith(directory.absolutePath)
