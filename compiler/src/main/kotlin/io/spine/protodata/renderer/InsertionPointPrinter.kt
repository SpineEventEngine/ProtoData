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

package io.spine.protodata.renderer

import com.google.common.base.Preconditions.checkPositionIndex
import io.spine.protodata.FileCoordinates
import io.spine.protodata.FileCoordinates.SpecCase.END_OF_FILE
import io.spine.protodata.FileCoordinates.SpecCase.INLINE
import io.spine.protodata.FileCoordinates.SpecCase.WHOLE_LINE
import io.spine.tools.code.Language

/**
 * A [Renderer] which adds [InsertionPoint]s to the code.
 *
 * Insertion points help the developers of other `Renderer`s by marking up source files for easier
 * code insertion. This way, it's only required to parse the file's contents once — in
 * an `InsertionPointPrinter`. The job of other `Renderer`s would be to insert their code into
 * pre-prepared insertion points.
 *
 * By default, there are no insertion points in the code files. To add some, create a subtype
 * of `InsertionPointPrinter` and add it to your processing as a first `Renderer`. Note that
 * the `InsertionPointPrinter`s need to be invoked before the other `Renderers`.
 */
public abstract class InsertionPointPrinter(
    private val target: Language
) : Renderer(target) {

    /**
     * [InsertionPoint]s which could be added to source code by this `InsertionPointPrinter`.
     *
     * The property getter may use [Renderer.select] to find out more info about the message types.
     */
    protected abstract fun supportedInsertionPoints(): Set<InsertionPoint>

    final override fun render(sources: SourceFileSet) {
        sources.prepareCode { file ->
            supportedInsertionPoints().forEach { point ->
                val text = file.text()
                val coordinates = point.locate(text)
                val lines = text.lines().toMutableList()
                when (coordinates.specCase) {
                    INLINE -> {
                        renderInlinePoint(coordinates, lines, point)
                    }
                    WHOLE_LINE -> {
                        val comment = target.comment(point.codeLine)
                        lines.checkLineNumber(coordinates.wholeLine)
                        lines.add(coordinates.wholeLine, comment)
                    }
                    END_OF_FILE -> {
                        val comment = target.comment(point.codeLine)
                        lines.add(comment)
                    }
                    else -> {} // No need to add anything.
                               // Insertion point should not appear in the file.
                }
                file.updateLines(lines)
            }
        }
    }

    private fun renderInlinePoint(
        coordinates: FileCoordinates,
        lines: MutableList<String>,
        point: InsertionPoint,
    ) {
        val position = coordinates.inline
        lines.checkLineNumber(position.line)
        val originalLine = lines[position.line]
        val lineStart = originalLine.substring(0, position.column)
        val lineEnd = originalLine.substring(position.column)
        val comment = target.comment(point.codeLine)
        val annotatedLine = lineStart + comment + lineEnd
        lines[position.line] = annotatedLine
    }
}

private fun List<String>.checkLineNumber(index: Int) =
    checkPositionIndex(index, size, "Line number")
