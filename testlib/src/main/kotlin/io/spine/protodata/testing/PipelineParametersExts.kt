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

package io.spine.protodata.testing

import io.spine.protodata.ast.toDirectory
import io.spine.protodata.ast.toAbsoluteFile
import io.spine.protodata.params.PipelineParameters
import io.spine.protodata.params.WorkingDirectory
import io.spine.tools.code.SourceSetName
import io.spine.validate.NonValidated
import java.io.File
import java.nio.file.Path

/**
 * Creates a partial instance of [PipelineParameters] by applying the [block] to the builder.
 */
public fun pipelineParams(
    block: (PipelineParameters.Builder.() -> Unit)
): @NonValidated PipelineParameters {
    val builder = PipelineParameters.newBuilder()
    block(builder)
    return builder.buildPartial()
}

/**
 * Assigns the given [file] as the reference to the request parameters file.
 */
public fun PipelineParameters.Builder.withRequestFile(file: File): PipelineParameters.Builder  {
    request = file.toAbsoluteFile()
    return this
}

/**
 * Assigns the given [file] as the reference to the request parameters file.
 */
public fun PipelineParameters.Builder.withRequestFile(file: Path): PipelineParameters.Builder {
    withRequestFile(file.toFile())
    return this
}

/**
 * Assigns the given [dir] as the reference to the settings directory.
 */
public fun PipelineParameters.Builder.withSettingsDir(dir: Path): PipelineParameters.Builder {
    settings = dir.toDirectory()
    return this
}

/**
 * Adds [sourceRoot] and [targetRoot] directories to this builder of parameters.
 */
public fun PipelineParameters.Builder.withRoots(
    sourceRoot: Path,
    targetRoot: Path
): PipelineParameters.Builder {
    val builder = this@withRoots
    builder.addSourceRoot(sourceRoot.toDirectory())
    builder.addTargetRoot(targetRoot.toDirectory())
    return this
}

/**
 * Creates a partial instance of [PipelineParameters] that refer to the given settings directory.
 */
public fun parametersWithSettingsDir(dir: Path): @NonValidated PipelineParameters =
    PipelineParameters.newBuilder()
        .setSettings(dir.toDirectory())
        .buildPartial()

/**
 * Creates a partial instance of [PipelineParameters] populating it according to the given
 * ProtoData working directory.
 *
 * Populated properties are:
 *  * The [request file][PipelineParameters.getRequest] name is given for
 *  the `testFixtures` source set.
 *  * The [settings directory][PipelineParameters.getSettings] is named
 *  after the [convention][WorkingDirectory.settingsDirectory].
 */
public fun parametersForWorkingDir(
    workingDir: Path
): @NonValidated PipelineParameters {
    val wd = WorkingDirectory(workingDir)
    val requestFile = wd.requestDirectory.file(SourceSetName("testFixtures"))
    val params = PipelineParameters.newBuilder()
        .setSettings(wd.settingsDirectory.path.toDirectory())
        .setRequest(requestFile.toAbsoluteFile())
        .buildPartial()
    return params
}
