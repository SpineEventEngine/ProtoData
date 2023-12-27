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

package io.spine.protodata.test

import io.spine.protodata.renderer.CoordinatesFactory.Companion.nowhere
import io.spine.protodata.renderer.InsertionPointPrinter
import io.spine.protodata.renderer.NonRepeatingInsertionPoint
import io.spine.text.Text
import io.spine.text.TextCoordinates
import io.spine.tools.code.Java
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * An [InsertionPointPrinter] for the [AnnotationInsertionPoint].
 */
public class AnnotationInsertionPointPrinter :
    InsertionPointPrinter<Java>(Java, AnnotationInsertionPoint.values().toSet())

/**
 * Insertion points that help renderers annotate certain parts of a Java file.
 */
public enum class AnnotationInsertionPoint : NonRepeatingInsertionPoint {

    /**
     * An insertion point in the imports black.
     *
     * This insertion point allows importing types into the Java file.
     */
    IMPORT {
        override fun locateOccurrence(text: Text): TextCoordinates {
            val lines = text.lines()
            val packageLineIndex = lines.asSequence()
                .mapIndexed { index, line -> index to line }
                .find { (_, line) -> line.startsWith("package") }
                ?.first
            val targetLine = if (packageLineIndex == null) 0 else packageLineIndex + 1
            return atLine(targetLine)
        }
    },

    /**
     * An insertion point before the return type of method called `foo`.
     *
     * This insertion point allows annotating the return type.
     */
    BEFORE_RETURN_TYPE_METHOD_FOO {
        override fun locateOccurrence(text: Text): TextCoordinates {
            val lines = text.lines()
            val (lineIndex, line) = lines.asSequence()
                .mapIndexed { index, line -> index to line }
                .find { (_, line) ->
                    line.matches(Regex(".*\\sfoo\\(\\)\\s\\{.*", DOT_MATCHES_ALL))
                }
                ?: return nowhere
            val matching = Regex("\\s([\\w.]*\\.)?(\\w+)\\sfoo\\(\\)\\s\\{").find(line)!!
            val matchedClass = matching.groupValues[2]
            val columnIndex = line.lastIndexOf(matchedClass)
            return at(lineIndex, columnIndex)
        }
    };

    override val label: String
        get() = name
}
