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

import io.spine.protodata.CLI_APP_CLASS
import io.spine.protodata.cli.ConfigFileParam
import io.spine.protodata.cli.PathsParam
import io.spine.protodata.cli.PluginParam
import io.spine.protodata.cli.RequestParam
import io.spine.protodata.cli.UserClasspathParam
import io.spine.protodata.gradle.SourcePaths
import io.spine.protodata.renderer.DefaultGenerator
import io.spine.tools.gradle.protobuf.containsProtoFiles
import java.io.File
import java.io.File.pathSeparator
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.SourceSet

/**
 * A task which executes a single ProtoData command.
 *
 * This class is public to allow users find ProtoData tasks by their type. This is useful
 * to configure task dependencies, enable and disable individual tasks, add conditions
 * via `onlyIf { }`, etc.
 *
 * Users should NOT change the CLI command, user directory, etc. directly.
 * Please refer to the `protoData { }` extension to configure ProtoData.
 */
public abstract class LaunchProtoData : JavaExec() {

    @get:InputFile
    internal lateinit var requestFile: Provider<RegularFile>

    @get:Internal
    public abstract val configurationFile: RegularFileProperty

    @get:Input
    internal lateinit var plugins: Provider<List<String>>

    @get:Input
    internal lateinit var optionProviders: Provider<List<String>>

    @get:Internal
    internal lateinit var paths: Set<SourcePaths>

    @get:InputFiles
    internal lateinit var userClasspathConfig: Configuration

    /**
     * A Gradle [Configuration] which is used to run ProtoData.
     */
    @get:InputFiles
    internal lateinit var protoDataConfig: Configuration

    @Suppress("unused") // Used by Gradle for incremental compilation.
    @get:InputFiles
    @get:Optional
    internal val sources: Set<Directory>
        get() = paths.asSequence()
            .map { it.source }
            .filter { it != null }
            .map { project.layout.projectDirectory.dir(it!!) }
            .toSet()

    @Suppress("unused") // Used by Gradle for incremental compilation.
    @get:OutputDirectories
    internal val targets: Set<Directory>
        get() = paths.asSequence()
            .map { it.target }
            .filter { it != null }
            .map { project.layout.projectDirectory.dir(it!!) }
            .toSet()

    /**
     * Configures the CLI command for this task.
     *
     * This method *must* be called after all the configuration is done for the task.
     */
    internal fun compileCommandLine() {
        val command = sequence {
            plugins.get().forEach {
                yield(PluginParam.name)
                yield(it)
            }
            yield(RequestParam.name)
            yield(project.file(requestFile).absolutePath)
            paths.forEach {
                yield(PathsParam.name)
                yield(it.toCliParam())
            }

            val userCp = userClasspathConfig.asPath
            if (userCp.isNotEmpty()) {
                yield(UserClasspathParam.name)
                yield(userCp)
            }

            if (configurationFile.isPresent) {
                yield(ConfigFileParam.name)
                yield(project.file(configurationFile).absolutePath)
            }
        }.asIterable()
        if (logger.isInfoEnabled) {
            logger.info("ProtoData command for `${path}`: ${command.joinToString(separator = " ")}")
        }
        classpath(protoDataConfig)
        classpath(userClasspathConfig)
        mainClass.set(CLI_APP_CLASS)
        args(command)
    }

    internal fun setPreLaunchCleanup() {
        doFirst(CleanAction())
    }

    /**
     * Cleans the target directory to prepare it for ProtoData.
     */
    private inner class CleanAction : Action<Task> {

        override fun execute(t: Task) {
            val sourceDirs = paths.map { File(it.source!!) }
            val targetDirs = paths.map { File(it.target!!) }

            if (sourceDirs.isEmpty()) {
                return
            }
            sourceDirs.asSequence()
                .zip(targetDirs.asSequence())
                .filter { (s, t) -> s != t }
                .map { it.second }
                .filter { it.exists() && it.list()!!.isNotEmpty() }
                .forEach {
                    logger.info("Cleaning target directory `$it`.")
                    project.delete(it)
                }
        }
    }
}

private fun SourcePaths.toCliParam(): String {
    checkAllSet()
    val label = if (generatorName != DefaultGenerator.name && generatorName.isNotBlank()) {
        "$language($generatorName)"
    } else {
        language
    }
    val sourcePath = source ?: ""
    val targetPath = target!!
    val parts = listOf(label, sourcePath, targetPath)
    return parts.joinToString(pathSeparator)
}

/**
 * Tells if the request file for this task exists.
 *
 * Logs error if the given source set contains `proto` directory which contains files,
 * which assumes that the request file should have been created.
 */
internal fun LaunchProtoData.checkRequestFile(sourceSet: SourceSet): Boolean {
    val requestFile = requestFile.get().asFile
    if (!requestFile.exists() && sourceSet.containsProtoFiles()) {
        project.logger.error(
            "Unable to locate the request file `$requestFile` which should have been created" +
                    " because the source set `${sourceSet.name}` contains `.proto` files." +
                    " The task `${name}` was skipped because the absence of the request file."
        )
    }
    return requestFile.exists()
}
