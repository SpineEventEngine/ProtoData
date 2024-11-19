/*
 * Copyright 2024, TeamDev. All rights reserved.
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
import io.spine.protodata.gradle.Names.PROTOBUF_GRADLE_PLUGIN_ID
import io.spine.protodata.gradle.Names.PROTODATA_PROTOC_PLUGIN
import io.spine.protodata.gradle.Names.PROTO_DATA_RAW_ARTIFACT
import io.spine.protodata.gradle.Names.USER_CLASSPATH_CONFIGURATION
import io.spine.protodata.gradle.ProtocPluginArtifact
import io.spine.protodata.gradle.plugin.GeneratedSubdir.GRPC
import io.spine.protodata.gradle.plugin.GeneratedSubdir.JAVA
import io.spine.protodata.gradle.plugin.GeneratedSubdir.KOTLIN
import io.spine.tools.code.manifest.Version
import io.spine.tools.gradle.project.sourceSets
import io.spine.tools.gradle.protobuf.generatedDir
import io.spine.tools.gradle.protobuf.generatedSourceProtoDir
import io.spine.tools.gradle.protobuf.protobufExtension
import io.spine.util.theOnly
import java.io.File
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.model.IdeaModule
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

    private val version: String by lazy {
        readVersion()
    }

    override fun apply(project: Project) {
        with(project) {
            createConfigurations(this@Plugin.version)
            createTasks()
            configureWithProtobufPlugin(this@Plugin.version)
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

private fun Project.createExtension(): Extension {
    val extension = Extension(this)
    extensions.add(CodegenSettings::class.java, EXTENSION_NAME, extension)
    return extension
}

/**
 * Obtains an instance of the project [Extension] added by ProtoData Gradle Plugin.
 *
 * Or, if the extension is not yet added, creates it and returns.
 */
internal val Project.extension: Extension
    get() = extensions.findByType(CodegenSettings::class)?.run { this as Extension }
        ?: createExtension()

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

    configurations.create(USER_CLASSPATH_CONFIGURATION) {
        it.exclude(group = Artifacts.group, module = Artifacts.compiler)
    }
}

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
private fun Project.createTasks() {
    val settingsDirTask = createSettingsDirTask()
    sourceSets.forEach { sourceSet ->
        createLaunchTask(settingsDirTask, sourceSet)
        createCleanTask(sourceSet)
    }
}

private fun Project.createSettingsDirTask(): CreateSettingsDirectory {
    val result = tasks.create<CreateSettingsDirectory>("createSettingsDirectory")
    return result
}

/**
 * Creates [LaunchProtoData] to serve the given [sourceSet].
 */
@CanIgnoreReturnValue
private fun Project.createLaunchTask(
    settingsDirTask: CreateSettingsDirectory,
    sourceSet: SourceSet
): LaunchProtoData {
    val taskName = LaunchTask.nameFor(sourceSet)
    val result = tasks.create<LaunchProtoData>(taskName) {
        applyDefaults(sourceSet, settingsDirTask)
    }
    return result
}

/**
 * Creates a task which deletes the generated code for the given [sourceSet].
 *
 * Makes a `clean` task depend on the created task.
 * Also, makes the task which launches ProtoData CLI depend on the created task.
 */
private fun Project.createCleanTask(sourceSet: SourceSet) {
    val project = this
    val cleanSourceSet = CleanTask.nameFor(sourceSet)
    tasks.create<Delete>(cleanSourceSet) {
        delete(extension.targetDirs(sourceSet))

        tasks.getByName("clean").dependsOn(this)
        val launchTask = LaunchTask.get(project, sourceSet)
        launchTask.mustRunAfter(this)
    }
}

private fun Project.configureWithProtobufPlugin(protoDataVersion: String) {
    val protocPlugin = ProtocPluginArtifact(protoDataVersion)
    pluginManager.withPlugin(PROTOBUF_GRADLE_PLUGIN_ID) {
        configureProtobufPlugin(protocPlugin)
    }
}

/**
 * Configures the Protobuf Gradle Plugin by adding ProtoData plugin to the list of `protoc` plugins.
 *
 * Also configures the `GenerateProtoTaskCollection` by adding a configuration action for each
 * of the tasks.
 */
private fun Project.configureProtobufPlugin(protocPlugin: ProtocPluginArtifact) {
    protobufExtension?.apply {
        plugins {
            it.create(PROTODATA_PROTOC_PLUGIN) { locator ->
                locator.artifact = protocPlugin.coordinates
            }
        }

        /* The below block adds a configuration action for the `GenerateProtoTaskCollection`.
           We cannot do it like `generateProtoTasks.all().forEach { ... }` because it
           breaks the configuration order of the `GenerateProtoTaskCollection`.
           This, in turn, leads to missing generated sources in the `compileJava` task. */
        generateProtoTasks {
            it.all().forEach { task ->
                configureProtoTask(task)
            }
        }
    }
}

/**
 * Configures the given [task] by enabling Kotlin code generation and adding and
 * configuring ProtoData `protoc` plugin for the task.
 *
 * The method also handles the exclusion of duplicated source code and task dependencies.
 *
 * @see [GenerateProtoTask.configureSourceSetDirs]
 * @see [Project.handleLaunchTaskDependency]
 */
private fun Project.configureProtoTask(task: GenerateProtoTask) {
    if (hasJavaOrKotlin()) {
        task.builtins.maybeCreate("kotlin")
    }
    val sourceSet = task.sourceSet
    task.plugins.apply {
        create(PROTODATA_PROTOC_PLUGIN) {
            val requestFile = extension.requestFile(sourceSet)
            val path = requestFile.get().asFile.absolutePath
            val nameEncoded = path.base64Encoded()
            it.option(nameEncoded)
            if (logger.isDebugEnabled) {
                logger.debug("The task `${task.name}` got plugin `$PROTODATA_PROTOC_PLUGIN`" +
                        " with the option `$nameEncoded`.")
            }
        }
    }
    task.configureSourceSetDirs()
    handleLaunchTaskDependency(task)
}

/**
 * Tells if this project can deal with Java code.
 *
 * @return `true` if `java` plugin is installed, `false` otherwise.
 */
private fun Project.hasJava(): Boolean =
    pluginManager.hasPlugin("java")

/**
 * Tells if this project can deal with Kotlin code.
 *
 * @return `true` if `compileKotlin` or `compileTestKotlin` tasks are present, `false` otherwise.
 */
private fun Project.hasKotlin(): Boolean {
    val compileKotlin = tasks.findByName("compileKotlin")
    val compileTestKotlin = tasks.findByName("compileTestKotlin")
    return compileKotlin != null || compileTestKotlin != null
}

/**
 * Verifies if the project can deal with Java or Kotlin code.
 *
 * The current Protobuf support of Kotlin is based on Java codegen.
 * Therefore, it is likely that Java would be enabled in the project for
 * Kotlin proto code to be generated.
 * Though, it may change someday, and Kotlin support for Protobuf would be
 * self-sufficient. This method assumes such a case when it checks the presence of
 * Kotlin compilation tasks.
 *
 * @see [hasJava]
 * @see [hasKotlin]
 */
private fun Project.hasJavaOrKotlin(): Boolean {
    if (hasJava()) {
        return true
    }
    return hasKotlin()
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
 * Obtains the `generated` directory for the given [sourceSet] and a language.
 *
 * If the language is not given, the returned directory is the root directory for the source set.
 */
private fun Project.generatedDir(sourceSet: SourceSet, language: String = ""): File {
    val path = "$targetBaseDir/${sourceSet.name}/$language"
    return File(path)
}

/**
 * The names of the subdirectories where ProtoData places generated files.
 */
private object GeneratedSubdir {
    const val JAVA = "java"
    const val KOTLIN = "kotlin"
    const val GRPC = "grpc"
}

/**
 * Exclude [GenerateProtoTask.outputBaseDir] from Java source set directories to avoid
 * duplicated source code files.
 *
 * Adds the `generated` directory to the Java and Kotlin source sets instead.
 */
private fun GenerateProtoTask.configureSourceSetDirs() {
    val protocOutputDir = File(outputBaseDir).parentFile

    /** Filters out directories belonging to `build/generated/source/proto`. */
    fun excludeFor(lang: SourceDirectorySet) {
        val newSourceDirectories = lang.sourceDirectories
            .filter { !it.residesIn(protocOutputDir) }
            .toSet()

        // Clear the source directories of the Java source set.
        // This trick was needed when building `base` module of Spine.
        // Otherwise, the `java` plugin would complain about duplicate source files.
        lang.setSrcDirs(listOf<String>())

        // Add the filtered directories back to the Java source set.
        lang.srcDirs(newSourceDirectories)
    }

    if (project.hasJava()) {
        val java = sourceSet.java
        excludeFor(java)

        java.srcDir(generatedDir(JAVA))

        // Add the `grpc` directory unconditionally.
        // We may not have all the `protoc` plugins configured for the task at this time.
        // So, we cannot check if the `grpc` plugin is enabled.
        // It is safe to add the directory anyway, because `srcDir()` does not require
        // the directory to exist.
        java.srcDir(generatedDir(GRPC))
    }

    if (project.hasKotlin()) {
        val kotlinDirectorySet = sourceSet.kotlinDirectorySet()
        kotlinDirectorySet!!.let {
            excludeFor(it)
            it.srcDirs(generatedDir(KOTLIN))
        }
    }
}

private fun SourceSet.kotlinDirectorySet(): SourceDirectorySet? =
    extensions.findByName("kotlin") as SourceDirectorySet?

/**
 * Obtains the `generated` directory for the source set of the task.
 *
 * If [language] is specified returns the subdirectory for this language.
 */
private fun GenerateProtoTask.generatedDir(language: String = ""): File =
    project.generatedDir(sourceSet, language)

/**
 * Makes a [LaunchProtoData], if it exists for the source set of the given [GenerateProtoTask],
 * depend on this task.
 *
 * If the [LaunchProtoData] task does not exist (which may be the case for custom source sets
 * created by other plugins), arranges the task creation on [Project.afterEvaluate].
 * In this case the [CleanTask] is also created with appropriate dependencies.
 */
private fun Project.handleLaunchTaskDependency(generateProto: GenerateProtoTask) {
    val sourceSet = generateProto.sourceSet
    var launchTask: Task? = LaunchTask.find(project, sourceSet)
    if (launchTask != null) {
        launchTask.dependsOn(generateProto)
    } else {
        project.afterEvaluate {
            val settingsTask =
                project.tasks.withType(CreateSettingsDirectory::class.java).theOnly()
            launchTask = createLaunchTask(settingsTask, sourceSet)
            launchTask!!.dependsOn(generateProto)
            createCleanTask(sourceSet)
        }
    }
}

/**
 * Ensures that the sources generated by Protobuf Gradle plugin are
 * not included in the IDEA project.
 *
 * IDEA should only see the sources generated by ProtoData as
 * we define in [GenerateProtoTask.configureSourceSetDirs].
 */
private fun Project.configureIdea() {
    val thisProject = this
    gradle.afterProject {
        if (it == thisProject) {
            pluginManager.withPlugin("idea") {
                val idea = extensions.getByType<IdeaModel>()
                idea.module.setupDirectories(thisProject)
            }
        }
    }
}

private fun IdeaModule.setupDirectories(project: Project,) {

    fun filterSources(sources: Set<File>, excludeDir: File): Set<File> =
        sources.filter { !it.residesIn(excludeDir) }.toSet()

    val protocOutput = project.file(project.generatedSourceProtoDir)
    excludeWithNested(protocOutput)
    sourceDirs = filterSources(sourceDirs, protocOutput)
    testSources.filter { !it.residesIn(protocOutput) }
    generatedSourceDirs = if (project.generatedDir.exists()) {
        //TODO:2024-11-19:alexander.yevsyukov: Update with the code from `ProtoTaskExtensions.kt`.
        project.generatedDir.listDirectoryEntries()
            .map { it.toFile() }
            .toSet()
    } else {
        emptySet<File>()
    }
}

/**
 * Excludes the given directory and its immediate subdirectories from
 * being seen as ones with the source code.
 *
 * The primary use of this extension is to exclude `build/generated/source/proto` and its
 * subdirectories to avoid duplication of types in the generated code with those in
 * produced by ProtoData under the `$projectDir/generated/` directory.
 */
private fun IdeaModule.excludeWithNested(directory: File) {
    if (directory.exists()) {
        excludeDirs.add(directory)
        directory.toPath().listDirectoryEntries().forEach {
            excludeDirs.add(it.toFile())
        }
    }
}

/**
 * Tells if this file resides in the given [directory].
 */
private fun File.residesIn(directory: File): Boolean =
    canonicalFile.startsWith(directory.absolutePath)
