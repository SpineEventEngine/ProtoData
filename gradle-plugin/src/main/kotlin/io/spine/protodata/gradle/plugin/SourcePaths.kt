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

package io.spine.protodata.gradle.plugin

import io.spine.protodata.renderer.Default
import io.spine.protodata.renderer.SourceGenerator
import io.spine.tools.code.Language
import java.io.File
import org.gradle.api.Named

public data class SourcePaths(
    public var source: String? = null,
    public var target: String? = null,
    public var language: String? = null,
    public var generatorName: String = Default.name
) : Named {

    public constructor(
        source: File,
        target: File,
        language: Language,
        generator: SourceGenerator
    ) : this(
        source.absolutePath,
        target.absolutePath,
        language::class.qualifiedName,
        generator.name
    )

    internal fun checkAllSet() {
        require(target.isNullOrBlank()) { missingMessage("target path") }
        require(language.isNullOrBlank()) { missingMessage("language") }
    }

    override fun getName(): String = generatorName

    private fun missingMessage(propName: String) = "Source file set `$name` requires the $propName."
}
