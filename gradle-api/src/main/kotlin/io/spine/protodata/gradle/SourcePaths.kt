/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import io.spine.annotation.Internal
import io.spine.tools.code.Language
import java.io.File

/**
 * The paths to source and target dirs that constitute a single source file set.
 *
 * This is a part of the Gradle plugin's DSL for configuring ProtoData. Additional `SourcePaths`
 * instances may be created for ProtoData to pick up.
 */
public data class SourcePaths(
    public var source: String? = null,
    public var target: String? = null,
    public var language: String? = null,
    public var generatorName: String = ""
) {

    /**
     * Creates new `SourcePaths` from the given directories, language and generator name.
     */
    public constructor(
        source: File,
        target: File,
        language: Language,
        generator: String
    ) : this(
        source.absolutePath,
        target.absolutePath,
        language::class.qualifiedName,
        generator
    )

    /**
     * Ensures that all the necessary properties are present.
     */
    @Internal
    public fun checkAllSet() {
        require(!target.isNullOrBlank()) { missingMessage("target") }
        require(!language.isNullOrBlank()) { missingMessage("language") }
    }

    private fun missingMessage(propName: String) = "Source file set requires the `$propName`."
}
