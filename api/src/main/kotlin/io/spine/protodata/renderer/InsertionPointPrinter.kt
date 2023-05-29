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

import io.spine.core.userId
import io.spine.protodata.event.insertionPointPrinted
import io.spine.protodata.filePath
import io.spine.server.integration.ThirdPartyContext
import io.spine.text.TextCoordinates
import io.spine.text.TextCoordinates.KindCase.END_OF_TEXT
import io.spine.text.TextCoordinates.KindCase.INLINE
import io.spine.text.TextCoordinates.KindCase.WHOLE_LINE
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
    protected val target: Language
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
                val coords = point.locate(text)
                val precedent = coords.precedentType()
                if (precedent != null) {
                    coords.ensureSameType(point, precedent)
                    val lines = file.lines().toMutableList()
                    when (precedent) {
                        INLINE -> renderInlinePoint(coords, lines, point, file)
                        WHOLE_LINE -> renderWholeLinePoint(coords, lines, point, file)
                        else -> error("Unexpected precedent type $precedent.")
                    }
                    file.updateLines(lines)
                }
            }
        }
    }

    private fun renderWholeLinePoint(
        coordinates: Set<TextCoordinates>,
        lines: MutableList<String>,
        point: InsertionPoint,
        file: SourceFile
    ) {
        val comment = target.comment(point.codeLine)
        val lineNumbers = coordinates.filter { it.hasWholeLine() }.map { it.wholeLine }
        lineNumbers.forEach {
            lines.checkLineNumber(it)
        }
        val correctedLineNumbers = lineNumbers.asSequence()
            .sorted()
            .mapIndexed { index, lineNumber -> lineNumber + index }
        correctedLineNumbers.forEach {
            lines.add(it, comment)
        }
        if (END_OF_FILE in coordinates) {
            lines.add(comment)
        }
        reportPoint(file, point.label, comment)
    }

    private fun renderInlinePoint(
        coordinates: Set<TextCoordinates>,
        lines: MutableList<String>,
        point: InsertionPoint,
        file: SourceFile
    ) {
        val cursors = coordinates.map { it.inline }
        val cursorsByLine = cursors.groupBy({ it.line }, { it.column })
        val comment = target.comment(point.codeLine)
        cursorsByLine.forEach { lineNumber, columns ->
            lines.checkLineNumber(lineNumber)
            val line = lines[lineNumber]
            columns.forEach {
                line.checkLinePosition(it)
            }
            val cols = columns.sorted()
            lines[lineNumber] = annotate(line, cols, comment)
            reportPoint(file, point.label, comment)
        }
    }

    private fun annotate(line: String, cols: List<Int>, comment: String) = buildString {
        append(line.substring(0, cols.first()))
        cols.forEachIndexed { index, column ->
            append(' ')
            append(comment)
            append(' ')
            val nextPart = if (index + 1 == cols.size) {
                line.substring(column)
            } else {
                line.substring(column, cols[index + 1])
            }
            append(nextPart)
        }
    }

    private fun reportPoint(sourceFile: SourceFile, pointLabel: String, comment: String) {
        val event = insertionPointPrinted {
            file = filePath { value = sourceFile.relativePath.toString() }
            label = pointLabel
            representationInCode = comment
        }
        InsertionPointPrinterContext.emittedEvent(event, actorId)
    }
}

private val actorId = userId { value = InsertionPointPrinter::class.qualifiedName!! }

private val InsertionPointPrinterContext = ThirdPartyContext.singleTenant("Insertion points")

private fun List<String>.checkLineNumber(index: Int) {
    if (index < 0 || index >= size) {
        throw RenderingException(
            "Line index $index is out of bounds. File contains $size lines.")
    }
}

private fun String.checkLinePosition(position: Int) {
    if (position < 0 || position >= length) {
        throw RenderingException(
            "Line does not have column $position: `$this`."
        )
    }
}

@Suppress("ReturnCount")
    // As this function is pretty small, it's easy to read even with 3 return statements.
private fun Iterable<TextCoordinates>.precedentType(): TextCoordinates.KindCase? {
    forEach { coords ->
        when (coords.kindCase) {
            INLINE -> return INLINE
            WHOLE_LINE, END_OF_TEXT -> return WHOLE_LINE
            else -> {} // Keep searching for the type.
        }
    }
    return null
}

private fun Iterable<TextCoordinates>.ensureSameType(insertionPoint: InsertionPoint,
                                                     definingType: TextCoordinates.KindCase) {
    forEach { coords ->
        if (!coords.compatibleWith(definingType)) {
            throw RenderingException(
                "One insertion point (${insertionPoint::class.qualifiedName}) cannot be " +
                        "whole-line and inline at the same time."
            )
        }
    }
}

private fun TextCoordinates.compatibleWith(type: TextCoordinates.KindCase) = when (kindCase) {
    INLINE, WHOLE_LINE -> type == kindCase
    END_OF_TEXT -> type == WHOLE_LINE
    else -> true
}
