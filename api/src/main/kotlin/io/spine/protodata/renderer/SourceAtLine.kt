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

package io.spine.protodata.renderer

import com.google.common.annotations.VisibleForTesting
import io.spine.string.Indent
import io.spine.string.Indent.Companion.DEFAULT_JAVA_INDENT_SIZE
import io.spine.string.Separator
import io.spine.string.atLevel

/**
 * A fluent builder for inserting code into pre-prepared insertion points.
 *
 * @see SourceFile.at
 */
public class SourceAtLine
internal constructor(
    private val file: SourceFile<*>,
    private val point: InsertionPoint,
    private val indent: Indent = Indent(DEFAULT_JAVA_INDENT_SIZE)
) {

    private var indentLevel: Int = 0

    /**
     * Specifies extra indentation to be added to inserted code lines
     *
     * Each unit adds the number of spaces specified by the [indent] property.
     */
    public fun withExtraIndentation(level: Int): SourceAtLine {
        require(level >= 0) { "Indentation level cannot be negative." }
        indentLevel = level
        return this
    }

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *         code lines.
     */
    public fun add(vararg lines: String): Unit =
        add(lines.toList())

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *         code lines.
     */
    public fun add(lines: Iterable<String>) {
        val text = file.code()
        val sourceLines = text.lines()
        val locations = point.locate(text).map { it.wholeLine }
        val newCode = lines.indent(indent, indentLevel)
        val newLines = newCode.lines()
        var alreadyInsertedCount = 0
        val updatedLines = ArrayList(sourceLines)
        sourceLines.forEachIndexed { index, _ ->
            if (locations.contains(index)) {
                //   1. Take the index of the insertion point before any new code is added.
                //   2. Add the number of lines taken up by the code inserted above this line.
                //   3. Insert code at the next line, after the insertion point.
                val trueLineNumber = index + (alreadyInsertedCount * newLines.size)
                updatedLines.addAll(trueLineNumber, newLines)
                alreadyInsertedCount++
            }
        }
        file.updateLines(updatedLines)
    }
}

/**
 * Joins these lines of code into a code block, accounting for extra indent.
 *
 * @param step
 *         the indentation of each level.
 * @param level
 *         the number of levels of indentation to add.
 *
 * @see <a href="https://github.com/SpineEventEngine/base/issues/809">Issue #809 in `base`</a>
 */
@VisibleForTesting
internal fun Iterable<String>.indent(step: Indent, level: Int): String {
    val indentation = step.atLevel(level)
    return joinToString(Separator.nl()) {
        indentation + it
    }
}
