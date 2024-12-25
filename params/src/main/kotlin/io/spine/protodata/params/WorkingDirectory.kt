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

import io.spine.protodata.params.Directories.PARAMETERS_SUBDIR
import io.spine.protodata.params.Directories.SETTINGS_SUBDIR
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.util.requireExistingDirectory
import java.nio.file.Path

/**
 * A directory which stores files passed to ProtoData command-line application.
 *
 * @param path The path to the existing directory.
 */
public class WorkingDirectory(
    public val path: Path
) {
    init {
        requireExistingDirectory(path)
    }

    /**
     * The directory managing files with [PipelineParameters].
     */
    public val parametersDirectory: ParametersDirectory by lazy {
        val dir = path.resolve(PARAMETERS_SUBDIR)
        dir.toFile().mkdirs()
        ParametersDirectory(dir)
    }

    /**
     * The directory managing files with plugin settings.
     */
    public val settingsDirectory: SettingsDirectory by lazy {
        val dir = path.resolve(SETTINGS_SUBDIR)
        dir.toFile().mkdirs()
        SettingsDirectory(dir)
    }
}
