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

package io.spine.protodata.render

import io.spine.core.userId
import io.spine.protodata.render.event.insertionPointPrinted
import io.spine.protodata.file
import io.spine.protodata.render.CoordinatesFactory.Companion.endOfFile
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
public abstract class InsertionPointPrinter<L: Language>(
    protected val target: L,
    points: Iterable<InsertionPoint>
) : Renderer<L>(target) {

    @Deprecated("Please pass the insertion points to the constructor.")
    public constructor(target: L) : this(target, emptyList())

    protected val points: Set<InsertionPoint>

    init {
        points.forEach {
            require(it.label.isNotEmpty()) {
                "The insertion point `$it` has en empty label and cannot be printed."
            }
        }
        this.points = points.toSet()
    }

    /**
     * [InsertionPoint]s which could be added to source code by this `InsertionPointPrinter`.
     *
     * The property getter may use [Renderer.select] to find out more info about the message types.
     */
    @Deprecated("Please pass the insertion points to the constructor.", ReplaceWith("points"))
    protected open fun supportedInsertionPoints(): Set<InsertionPoint> = points

    final override fun render(sources: SourceFileSet) {
        sources.prepareCode { file ->
            @Suppress("DEPRECATION") // Still have to support the deprecated method.
            supportedInsertionPoints()
                .filter { it.label.isNotEmpty() }
                .forEach { point ->
                    print(file, point)
                }
        }
    }

    private fun print(
        file: SourceFile<*>,
        point: InsertionPoint
    ) {
        val text = file.code()
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

    private fun renderWholeLinePoint(
        coordinates: Set<TextCoordinates>,
        lines: MutableList<String>,
        point: InsertionPoint,
        file: SourceFile<*>
    ) {
        val comment = target.comment(point.codeLine)
        val lineNumbers = coordinates
            .filter { it.hasWholeLine() }
            .map { it.wholeLine }
        lineNumbers.forEach {
            lines.checkLineNumber(it)
        }
        val correctedLineNumbers = lineNumbers.asSequence()
            .sorted()
            .mapIndexed { index, lineNumber -> lineNumber + index }
        correctedLineNumbers.forEach {
            lines.add(it, comment)
        }
        if (endOfFile in coordinates) {
            lines.add(comment)
        }
        reportPoint(file, point.label, comment)
    }

    private fun renderInlinePoint(
        coordinates: Set<TextCoordinates>,
        lines: MutableList<String>,
        point: InsertionPoint,
        file: SourceFile<*>
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

    private fun reportPoint(sourceFile: SourceFile<*>, pointLabel: String, comment: String) {
        val event = insertionPointPrinted {
            file = file { path = sourceFile.relativePath.toString() }
            label = pointLabel
            representationInCode = comment
        }
        check(context != null) {
            "Insertion point printer `$this` is not registered with a `CodegenContext`."
        }
        context!!.insertionPointsContext.emittedEvent(event, actorId)
    }

    private companion object {
        val actorId = userId { value = InsertionPointPrinter::class.qualifiedName!! }
    }
}

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

/**
 * Finds the precedent type for all the given coordinates.
 *
 * A precedent type is the common kind of all the coordinates. It can be either `WHOLE_LINE` or
 * `INLINE`. `END_OF_TEXT` coordinates are considered `WHOLE_LINE`. If none of the coordinates
 * define a particular place in the text, i.e. all have the `NOWHERE` kind, the precedent is not
 * defined and `null` is returned.
 *
 * This method does not look through all the coordinates. It stops the search as soon as at least
 * one non-`NOWHERE` instance is found. This method does not guarantee that all the coordinates
 * are compatible with the found precedent type.
 */
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

/**
 * Checks if all the receiver coordinates are [compatible][compatibleWith]
 * the given [precedentType].
 */
private fun Iterable<TextCoordinates>.ensureSameType(
    insertionPoint: InsertionPoint,
    precedentType: TextCoordinates.KindCase
) {
    forEach { coords ->
        if (!coords.compatibleWith(precedentType)) {
            throw RenderingException(
                "One insertion point (${insertionPoint::class.qualifiedName}) cannot be " +
                        "whole-line and inline at the same time."
            )
        }
    }
}

/**
 * Checks if these `TextCoordinates` are compatible with the given [precedentType].
 *
 * `TextCoordinates` of kinds `WHOLE_LINE` or `END_OF_TEXT` are compatible with the `WHOLE_LINE`
 * precedent type. `TextCoordinates` of kind `INLINE` are compatible with the `INLINE` precedent
 * type. `TextCoordinates` of kind `NOWHERE` are compatible with any type.
 */
private fun TextCoordinates.compatibleWith(
    precedentType: TextCoordinates.KindCase
) = when (kindCase) {
    INLINE, WHOLE_LINE -> precedentType == kindCase
    END_OF_TEXT -> precedentType == WHOLE_LINE
    else -> true
}
