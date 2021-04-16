/*
 * Copyright 2021, TeamDev. All rights reserved.
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
import io.spine.protodata.language.Language

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
) : Renderer(setOf(target)) {

    /**
     * [InsertionPoint]s which could be added to source code by this `InsertionPointPrinter`.
     *
     * The property getter may use [Renderer.select] to find out more info about the message types.
     */
    protected abstract val supportedInsertionPoints: Set<InsertionPoint>

    final override fun doRender(sources: SourceSet) {
        sources.prepareCode { file ->
            val lines = file.lines().toMutableList()
            supportedInsertionPoints.forEach { point ->
                val lineNumber = point.locate(lines)
                val comment = target.comment(point.codeLine)
                when(lineNumber) {
                    is LineIndex -> {
                        val index = lineNumber.value.toInt()
                        checkPositionIndex(index, lines.size, "Line number")
                        lines.add(index, comment)
                    }
                    is EndOfFile -> lines.add(comment)
                    is Nowhere -> {} // No need to add anything.
                                     // Insertion point should not appear in the file.
                }
            }
            file.updateLines(lines)
        }
    }
}
