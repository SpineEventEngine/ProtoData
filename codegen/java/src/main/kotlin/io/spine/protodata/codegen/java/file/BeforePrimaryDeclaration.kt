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
import io.spine.protodata.FileCoordinates
import io.spine.protodata.renderer.InsertionPoint
import io.spine.text.Text
import java.lang.System.lineSeparator

/**
 * A pattern matching a top-level Java declaration.
 *
 * This pattern matches a type declaration, such as a class, an interface, an annotation,
 * or an enum.
 *
 * The pattern expects the matched string to have at least one empty space character after
 * the keyword declaring the type. However, regular expression pattern matching cannot guarantee
 * that there will be no false positives, i.e. no structure, such as a comment, is going to yield
 * an unwanted match.
 */
private val pattern = Regex("((class)|(@?interface)|(enum))\\s+")
/**
 * An insertion point located just before the primary declaration of a Java file.
 *
 * The primary declaration is the top-level class, interface, annotation, or an enum type,
 * which matches by name with the class.
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

    override fun locate(text: Text): FileCoordinates {
        var isBlockComment = false
        val lines = text.lines()
        lines.forEachIndexed { index, line ->
            if (line.contains("/*")) {
                isBlockComment = true
            }
            if (line.contains("*/")) {
                isBlockComment = false
            }
            if (!isBlockComment && pattern.find(line) != null) {
                return atLine(index)
            }
        }
        _warn().log("Could not find the primary declaration in code:" + lineSeparator() +
                lines.joinToString(separator = lineSeparator()))
        return nowhere()
    }
}

