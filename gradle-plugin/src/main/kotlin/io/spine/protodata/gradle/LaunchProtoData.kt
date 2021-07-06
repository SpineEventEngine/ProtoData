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

import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Internal

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
public open class LaunchProtoData : Exec() {

    @get:Internal
    internal lateinit var protoDataExecutable: String

    @get:Internal
    internal lateinit var renderers: Provider<List<String>>

    @get:Internal
    internal lateinit var plugins: Provider<List<String>>

    @get:Internal
    internal lateinit var optionProviders: Provider<List<String>>

    @get:Internal
    internal lateinit var requestFile: Provider<RegularFile>

    @get:Internal
    internal lateinit var source: Provider<Directory>

    @get:Internal
    internal lateinit var target: Provider<Directory>

    @get:Internal
    internal lateinit var userClasspath: Provider<String>

    init {
        outputs.upToDateWhen { !requestFile.get().asFile.exists() }
        isIgnoreExitValue = false
    }

    /**
     * Configures the CLI command for this task.
     *
     * This method *must* be called after all the configuration is done for the task.
     */
    internal fun compileCommandLine() {
        commandLine(sequence {
            yield(protoDataExecutable)
            plugins.get().forEach {
                yield("--plugin")
                yield(it)
            }
            renderers.get().forEach {
                yield("--renderer")
                yield(it)
            }
            optionProviders.get().forEach {
                yield("--options")
                yield(it)
            }
            yield("--request")
            yield(requestFile.get().asFile.absolutePath)

            yield("--source-root")
            yield(source.absolutePath)

            yield("--target-root")
            yield(target.absolutePath)

            val userCp = userClasspath.get()
            if (userCp.isNotEmpty()) {
                yield("--user-classpath")
                yield(userCp)
            }
        }.asIterable())
    }
}

private val Provider<out FileSystemLocation>.absolutePath: String
    get() = get().asFile.absolutePath
