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

package io.spine.protodata.params

import io.spine.protodata.util.Format
import io.spine.protodata.util.extensions
import io.spine.protodata.util.parseFile
import io.spine.protodata.util.requireExistingDirectory
import io.spine.tools.code.SourceSetName
import io.spine.type.toJson
import java.io.File
import java.nio.file.Path

/**
 * A directory under [WorkingDirectory] which manages files of [PipelineParameters].
 */
public class ParametersDirectory(
    public val path: Path
) {

    init {
        requireExistingDirectory(path)
    }

    /**
     * Reads the parameters for the pipeline for the given source set.
     */
    public fun read(sourceSet: SourceSetName): PipelineParameters {
        val file = file(sourceSet)
        val result = parseFile(file, PipelineParameters::class.java)
        return result
    }

    /**
     * Creates the file storing the parameters for the pipeline for the given source set.
     */
    public fun write(sourceSet: SourceSetName, parameters: PipelineParameters): File {
        val content = parameters.toJson()
        val file = file(sourceSet)
        file.writeText(content)
        return file
    }

    /**
     * Obtains the file for passing parameters for compilation of the specified source set.
     */
    public fun file(sourceSet: SourceSetName): File {
        val fileName = "${sourceSet.value}.${DEFAULT_FORMAT.extensions.first()}"
        return path.resolve(fileName).toFile()
    }

    private companion object {
        val DEFAULT_FORMAT = Format.PROTO_JSON
    }
}
