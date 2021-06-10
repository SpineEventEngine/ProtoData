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
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile

public open class LaunchProtoData : Exec() {

    @get:Input
    internal lateinit var protoDataExecutable: String

    @get:Input
    internal lateinit var renderers: List<String>

    @get:Input
    internal lateinit var plugins: List<String>

    @get:Input
    internal lateinit var optionProviders: List<String>

    @get:InputFile
    internal lateinit var requestFile: RegularFile

    @get:InputDirectory
    internal lateinit var source: Directory

    @get:Input
    internal lateinit var userClasspath: String

    internal fun compileCommandLine() {
        commandLine(sequence {
            yield(protoDataExecutable)
            plugins.forEach {
                yield("--plugin")
                yield(it)
            }
            renderers.forEach {
                yield("--renderer")
                yield(it)
            }
            optionProviders.forEach {
                yield("--options")
                yield(it)
            }
            yield("--request")
            yield(requestFile.asFile.absolutePath)

            yield("--src")
            yield(source.asFile.absolutePath)
            if (userClasspath.isNotEmpty()) {
                yield("--user-classpath")
                yield(userClasspath)
            }
        }.asIterable())
    }
}
