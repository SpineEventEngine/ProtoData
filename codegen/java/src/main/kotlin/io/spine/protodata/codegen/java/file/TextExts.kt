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

package io.spine.protodata.codegen.java.file

import io.spine.string.Separator
import io.spine.string.trimWhitespace
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.text.textCoordinates

/**
 * Locates the pattern point in the given text checking that it is
 * not inside a Java comment line or block.
 *
 * @return the coordinates of the pattern or `null` if the pattern was not found.
 */
internal fun Text.locateOutsideComments(pattern: Regex): TextCoordinates? {
    var isBlockComment = false
    lines().forEachIndexed { index, line ->
        val trimmed = line.trimWhitespace()
        if (trimmed.startsWith("//")) {
            return@forEachIndexed
        }
        if (trimmed.startsWith("/*")) {
            isBlockComment = true
        }
        if (line.contains("*/")) {
            isBlockComment = false
        }
        if (!isBlockComment && pattern.find(line) != null) {
            return textCoordinates {
                wholeLine = index
            }
        }
    }
    return null
}

/**
 * Prints the lines of the text into a single string using the system line separator.
 */
internal fun Text.printLines(): String =
        lines().joinToString(separator = Separator.system.value)
