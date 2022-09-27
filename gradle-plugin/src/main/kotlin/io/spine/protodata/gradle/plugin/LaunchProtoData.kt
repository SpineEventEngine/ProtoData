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

import java.io.File.pathSeparator
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory

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

    @get:Input
    internal lateinit var renderers: Provider<List<String>>

    @get:Input
    internal lateinit var plugins: Provider<List<String>>

    @get:Input
    internal lateinit var optionProviders: Provider<List<String>>

    @get:InputFile
    internal lateinit var requestFile: Provider<RegularFile>

    /**
     * The path to the directory with the generated source code.
     *
     * May not be available, if `protoc` built-ins were turned off, resulting in no source code
     * being generated. In such a mode `protoc` worked only generating descriptor set files.
     *
     * This property is deprecated. [sources] should be used instead of it. Accessing this property
     * delegates to [sources].
     */
    @get:InputDirectory
    @get:Optional
    @Deprecated("Use `sources` instead.")
    internal var source: Provider<Directory>
        set(value) {
            sources = value.map { listOf(it) }
        }
        get() {
            return sources.map { it.first() }
        }

    /**
     * The path to the directory with the processed source code.
     *
     * This property is deprecated. [targets] should be used instead of it. Accessing this property
     * delegates to [targets].
     */
    @get:OutputDirectory
    @Deprecated("Use `targets` instead.")
    internal var target: Provider<Directory>
        set(value) {
            targets = value.map { listOf(it) }
        }
        get() {
            return targets.map { it.first() }
        }

    /**
     * The paths to the directories with the generated source code.
     *
     * May not be available, if `protoc` built-ins were turned off, resulting in no source code
     * being generated. In such a mode `protoc` worked only generating descriptor set files.
     */
    @get:InputFiles
    @get:Optional
    internal lateinit var sources: Provider<List<Directory>>

    /**
     * The paths to the directories where the source code processed by ProtoData should go.
     */
    @get:OutputDirectories
    internal lateinit var targets: Provider<List<Directory>>

    @get:InputFiles
    internal lateinit var userClasspathConfig: Configuration

    @get:InputFiles
    internal lateinit var protoDataConfig: Configuration

    @get:Internal
    public abstract val configuration: RegularFileProperty

    /**
     * Configures the CLI command for this task.
     *
     * This method *must* be called after all the configuration is done for the task.
     */
    internal fun compileCommandLine() {
        val command = sequence {
            plugins.get().forEach {
                yield("--plugin")
                yield(it)
            }
            renderers.get().forEach {
                yield("--renderer")
                yield(it)
            }
            optionProviders.get().forEach {
                yield("--option-provider")
                yield(it)
            }
            yield("--request")
            yield(project.file(requestFile).absolutePath)

            if (sources.isPresent) {
                yield("--source-root")
                yield(sources.absolutePaths())
            }

            yield("--target-root")
            yield(targets.absolutePaths())

            val userCp = userClasspathConfig.asPath
            if (userCp.isNotEmpty()) {
                yield("--user-classpath")
                yield(userCp)
            }

            if (configuration.isPresent) {
                yield("--configuration-file")
                yield(project.file(configuration).absolutePath)
            }

            yield("--ignore-missing")
        }.asIterable()
        if (logger.isDebugEnabled) {
            logger.debug("ProtoData command for ${path}: ${command.joinToString(separator = " ")}")
        }
        classpath(protoDataConfig)
        classpath(userClasspathConfig)
        mainClass.set("io.spine.protodata.cli.MainKt")
        args(command)

        doFirst(CleanAction())
    }

    /**
     * Cleans the target directory to prepare it for ProtoData.
     */
    private inner class CleanAction : Action<Task> {

        override fun execute(t: Task) {
            val sourceDirs = sources.absoluteDirs()
            val targetDirs = targets.absoluteDirs()

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

private fun Provider<List<Directory>>.absoluteDirs() = takeIf { it.isPresent }
    ?.get()
    ?.map { it.asFile.absoluteFile }
    ?: listOf()

private fun Provider<List<Directory>>.absolutePaths(): String =
    absoluteDirs().joinToString(pathSeparator)
