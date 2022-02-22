/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.logging.Logging
import io.spine.protodata.renderer.InsertionPoint
import io.spine.protodata.renderer.LineNumber
import io.spine.protodata.renderer.LineNumber.Companion.notInFile
import java.lang.System.lineSeparator

private val pattern = Regex(
    "(public\\s+)?(final\\s+)?(abstract\\s+)?((class)|(@?interface)|(enum))\\s+"
)

/**
 * An insertion point located just before the primary declaration of a Java file.
 *
 * The primary declaration is the top-level class, interface, or annotation type, which matches by
 * name with the class.
 *
 * While technically Java allows other top-level declarations is the same file, those are rarely
 * used. `BeforePrimaryDeclaration` does not account for such declarations when searching for
 * a line number.
 *
 * This insertion point is not bound to the contents of the file in `label`, thus allowing this type
 * to be an object.
 */
internal object BeforePrimaryDeclaration : InsertionPoint, Logging {

    override val label: String
        get() = this.javaClass.simpleName

    override fun locate(lines: List<String>): LineNumber {
        var isComment = false
        lines.forEachIndexed { index, line ->
            if (line.contains("/*")) {
                isComment = true
            }
            if (line.contains("*/")) {
                isComment = false
            }
            if (!isComment && pattern.find(line) != null) {
                return LineNumber.at(index)
            }
        }
        _warn().log("Could not find the primary declaration in code:" + lineSeparator() +
                lines.joinToString(separator = lineSeparator()))
        return notInFile()
    }
}
