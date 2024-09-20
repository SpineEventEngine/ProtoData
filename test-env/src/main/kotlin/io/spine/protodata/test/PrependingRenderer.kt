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

package io.spine.protodata.test

import io.spine.protodata.render.InsertionPoint
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.test.GenericInsertionPoint.FILE_START
import io.spine.tools.code.Java
import io.spine.util.theOnly
import kotlin.io.path.name

/**
 * A renderer that writes preset text into a given insertion point.
 */
public class PrependingRenderer(
    private val insertionPoint: InsertionPoint = FILE_START,
    private val inline: Boolean = false
) : Renderer<Java>(Java) {

    override fun render(sources: SourceFileSet) {
        val files = sources.filter {
            it.relativePath.name.endsWith("_.java")
        }
        require(files.size <= 1) {
            "Only expected one fitting file for test. Got: ${files.joinToString()}."
        }
        if (files.isNotEmpty()) {
            val file = files.theOnly()
            val content = "Hello from ${this.javaClass.name}"
            if (inline) {
                file.atInline(insertionPoint)
                    .add(content)
            } else {
                file.at(insertionPoint)
                    .add(content)
            }
        }
    }
}
