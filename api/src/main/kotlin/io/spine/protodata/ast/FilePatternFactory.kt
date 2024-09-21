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

package io.spine.protodata.ast

import org.checkerframework.checker.regex.qual.Regex

/**
 * Provides methods for creating [FilePattern] instances of sorts.
 */
public object FilePatternFactory {

    /**
     * Creates a new [FilePattern] with the [suffix][FilePattern.getSuffix] field filled.
     */
    public fun suffix(value: String): FilePattern = filePattern {
        value.checkNotBlank("suffix")
        suffix = value
    }

    /**
     * Creates a new [FilePattern] with the [prefix][FilePattern.getPrefix] field filled.
     */
    public fun prefix(value: String): FilePattern = filePattern {
        value.checkNotBlank("prefix")
        prefix = value
    }

    /**
     * Creates a new [FilePattern] with the [regex][FilePattern.getRegex] field filled.
     */
    public fun regex(pattern: @Regex String): FilePattern = filePattern {
        pattern.checkNotBlank("regex")
        regex = pattern
    }

    private fun String.checkNotBlank(name: String) {
        require(isNotBlank()) {
            "File pattern $name cannot be empty or blank: `$this`."
        }
    }
}
