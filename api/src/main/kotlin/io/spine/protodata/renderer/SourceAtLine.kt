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

package io.spine.protodata.renderer

import com.google.common.base.Preconditions

/**
 * A fluent builder for inserting code into pre-prepared insertion points.
 *
 * @see SourceFile.at
 */
public class SourceAtLine
internal constructor(
    private val file: SourceFile,
    private val point: InsertionPoint
) {

    private var indentLevel: Int = 0

    /**
     * Specifies extra indentation to be added to inserted code lines
     *
     * Each unit adds four spaces.
     */
    public fun withExtraIndentation(level: Int): SourceAtLine {
        Preconditions.checkArgument(level >= 0, "Indentation level cannot be negative.")
        indentLevel = level
        return this
    }

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *      code lines
     */
    public fun add(vararg lines: String): Unit =
        add(lines.toList())

    /**
     * Adds the given code lines at the associated insertion point.
     *
     * @param lines
     *      code lines
     */
    public fun add(lines: Iterable<String>) {
        val sourceLines = file.lines()
        val updatedLines = ArrayList(sourceLines)
        val pointMarker = point.codeLine
        val newCode = lines.linesToCode(indentLevel)
        sourceLines.mapIndexed { index, line -> index to line }
                   .filter { (_, line) -> line.contains(pointMarker) }
                   .map { it.first + 1 }
                   .forEach { index -> updatedLines.add(index, newCode) }
        file.updateLines(updatedLines)
    }
}

private fun Iterable<String>.linesToCode(indentLevel: Int): String =
    joinToString(System.lineSeparator()) {
        INDENT.repeat(indentLevel) + it
    }

private const val INDENT: String = "    "
