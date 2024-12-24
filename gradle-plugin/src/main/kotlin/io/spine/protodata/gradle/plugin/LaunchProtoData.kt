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

package io.spine.protodata.gradle.plugin

import com.intellij.openapi.util.io.FileUtil
import io.spine.protodata.Constants.CLI_APP_CLASS
import io.spine.protodata.cli.PluginParam
import io.spine.protodata.cli.ProtoFilesParam
import io.spine.protodata.cli.RequestParam
import io.spine.protodata.cli.SettingsDirParam
import io.spine.protodata.cli.SourceRootParam
import io.spine.protodata.cli.TargetRootParam
import io.spine.protodata.cli.UserClasspathParam
import io.spine.protodata.gradle.Names.PROTO_DATA_RAW_ARTIFACT
import io.spine.protodata.gradle.Names.USER_CLASSPATH_CONFIGURATION
import io.spine.protodata.gradle.error
import io.spine.protodata.gradle.info
import io.spine.tools.gradle.protobuf.containsProtoFiles
import java.io.File.pathSeparator
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.SourceSet

/**
 * A task which executes a single ProtoData command.
 *
 * This class is public to allow users to find ProtoData tasks by their type.
 * This is useful to configure task dependencies, enable and disable individual tasks,
 * add conditions via `onlyIf { }`, etc.
 *
 * Users should NOT change the CLI command, user directory, etc. directly.
 * Please refer to the `protoData { }` extension configuring ProtoData.
 */
public abstract class LaunchProtoData : JavaExec() {

    @get:Input
    internal abstract val sourceSetName: Property<String>

    /**
     * The file containing the binary form of `CodeGeneratorRequest` passed to this task.
     */
    @get:InputFile
    internal abstract val requestFile: RegularFileProperty

    /**
     * The directory which stores ProtoData settings files.
     */
    @get:InputDirectory
    internal abstract val settingsDir: DirectoryProperty

    @get:Input
    internal lateinit var plugins: Provider<List<String>>

    /**
     * The paths to the directories with the generated source code.
     *
     * May not be available, if `protoc` built-ins were turned off, resulting in no source code
     * being generated. In such a mode `protoc` worked only generating descriptor set files.
     */
    @get:InputFiles
    @get:Optional
    internal lateinit var sources: Provider<List<Directory>>

    @get:InputFiles
    internal lateinit var userClasspathConfiguration: Configuration

    /**
     * A Gradle [Configuration] which is used to run ProtoData.
     */
    @get:InputFiles
    internal lateinit var protoDataConfiguration: Configuration

    /**
     * The paths to the directories where the source code processed by ProtoData should go.
     */
    @get:OutputDirectories
    internal lateinit var targets: Provider<List<Directory>>

    /**
     * Configures the CLI command for this task.
     *
     * This method *must* be called after all the configuration is done for the task.
     */
    internal fun compileCommandLine() {
        val command = sequence {
            val protoFileList = project.protoFileList(sourceSetName.get())
            yield(ProtoFilesParam.name)
            yield(protoFileList.canonicalPath)

            plugins.get().forEach {
                yield(PluginParam.name)
                yield(it)
            }
            yield(RequestParam.name)
            yield(project.file(requestFile).absolutePath)

            if (sources.isPresent) {
                yield(SourceRootParam.name)
                yield(sources.absolutePaths())
            }

            yield(TargetRootParam.name)
            yield(targets.absolutePaths())

            val userCp = userClasspathConfiguration.asPath
            if (userCp.isNotEmpty()) {
                yield(UserClasspathParam.name)
                yield(userCp)
            }

            yield(SettingsDirParam.name)
            yield(project.file(settingsDir).absolutePath)
        }.asIterable()
        logger.info { "ProtoData command for `${path}`: ${command.joinToString(separator = " ")}" }
        classpath(protoDataConfiguration)
        classpath(userClasspathConfiguration)
        mainClass.set(CLI_APP_CLASS)
        args(command)
    }


    internal fun setPreLaunchCleanup() {
        doFirst(CleanTargetDirs())
    }

    /**
     * Cleans the target directory to prepare it for ProtoData.
     */
    private inner class CleanTargetDirs : Action<Task> {

        override fun execute(t: Task) {
            val sourceDirs = sources.absoluteDirs()
            val targetDirs = targets.absoluteDirs()

            if (sourceDirs.isEmpty()) {
                return
            }
            sourceDirs.asSequence()
                .zip(targetDirs.asSequence())
                .filter { (s, t) ->
                    // Do not clean directories if we are overwriting files in
                    // the directories created by `protoc`.
                    // Such a mode is deprecated currently, but we may revisit this later.
                    !FileUtil.filesEqual(s, t) /* Honor case-sensitivity under macOS. */
                }
                .map { it.second }
                .filter { it.exists() && it.list()?.isNotEmpty() ?: false }
                .forEach {
                    logger.info { "Cleaning target directory `$it`." }
                    project.delete(it)
                }
        }
    }
}

/**
 * Applies default settings to the receiver task.
 *
 * @param sourceSet The source set for which the task is created.
 * @param settingsDirTask The task for creating the settings directory from which this task depends.
 */
internal fun LaunchProtoData.applyDefaults(
    sourceSet: SourceSet,
    settingsDirTask: CreateSettingsDirectory
) {
    sourceSetName.set(sourceSet.name)
    val project = project
    settingsDir.set(settingsDirTask.settingsDir.get())
    val ext = project.extension
    plugins = ext.plugins
    requestFile.set(ext.requestFile(sourceSet))
    protoDataConfiguration = project.protoDataRawArtifact
    userClasspathConfiguration = project.userClasspath
    project.afterEvaluate {
        sources = ext.sourceDirs(sourceSet)
        targets = ext.targetDirs(sourceSet)
        compileCommandLine()
    }
    setPreLaunchCleanup()
    onlyIf {
        hasRequestFile(sourceSet)
    }
    setDependencies(sourceSet, settingsDirTask)
}

private fun LaunchProtoData.setDependencies(
    sourceSet: SourceSet,
    settingsDirTask: CreateSettingsDirectory
) {
    val project = project
    dependsOn(
        settingsDirTask,
        project.protoDataRawArtifact.buildDependencies,
        project.userClasspath.buildDependencies,
    )
    val launchTask = this
    project.javaCompileFor(sourceSet)?.dependsOn(launchTask)
    project.kotlinCompileFor(sourceSet)?.dependsOn(launchTask)
}

/**
 * Tells if the request file for this task exists.
 *
 * Logs error if the given source set contains `proto` directory which contains files,
 * which assumes that the request file should have been created.
 */
internal fun LaunchProtoData.hasRequestFile(sourceSet: SourceSet): Boolean {
    val requestFile = requestFile.get().asFile
    if (!requestFile.exists() && sourceSet.containsProtoFiles()) {
        logger.error {
            "Unable to locate the request file `$requestFile` which should have been created" +
                    " because the source set `${sourceSet.name}` contains `.proto` files." +
                    " The task `${name}` was skipped because the absence of the request file."
        }
    }
    return requestFile.exists()
}

private val Project.protoDataRawArtifact: Configuration
    get() = configurations.getByName(PROTO_DATA_RAW_ARTIFACT)

private val Project.userClasspath: Configuration
    get() = configurations.getByName(USER_CLASSPATH_CONFIGURATION)

private fun Provider<List<Directory>>.absoluteDirs() = takeIf { it.isPresent }
    ?.get()
    ?.map { it.asFile.absoluteFile }
    ?: listOf()

private fun Provider<List<Directory>>.absolutePaths(): String =
    absoluteDirs().joinToString(pathSeparator)
