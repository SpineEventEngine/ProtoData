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

package io.spine.protodata.settings

import io.spine.io.Glob
import io.spine.protodata.settings.Format.UNRECOGNIZED
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Checks if the given file matches this configuration format.
 */
public fun Format.matches(file: Path): Boolean =
    extensions
            .map { Glob.extension(it) }
            .any { it.matches(file) }


/**
 * Obtains file extensions associated with this format.
 */
public val Format.extensions: List<String>
    get() = if (this == UNRECOGNIZED) {
        emptyList()
    } else {
        valueDescriptor.options.getExtension(SettingsProto.extension).toList()
    }

/**
 * Obtains a [Format] from the file extension of the given configuration file.
 *
 * @throws IllegalStateException If the format is not recognized.
 */
public fun formatOf(file: Path): Format =
    Format.values().find { it.matches(file) }
        ?: error("Unrecognized settings format: `${file.name}`.")

/**
 * Tells if this file is a settings file.
 */
public fun Path.isSettings(): Boolean =
    Format.values().any { it.matches(this) }
