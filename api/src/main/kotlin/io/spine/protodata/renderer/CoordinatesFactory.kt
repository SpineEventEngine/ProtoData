/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import com.google.protobuf.Empty
import io.spine.annotation.Internal
import io.spine.text.TextCoordinates
import io.spine.text.cursor
import io.spine.text.textCoordinates

/**
 * A factory of [TextCoordinates] instances.
 *
 * This interface serves as a trait for the [InsertionPoint] type.
 * The methods it provides are meant to be used by the authors of custom insertion points.
 */
@Internal
public sealed interface CoordinatesFactory {

    /**
     * Obtains coordinates pointing at a specific line and column in the file.
     */
    public fun at(line: Int, column: Int): TextCoordinates {
        val cursor = cursor {
            this.line = line
            this.column = column
        }
        return textCoordinates {
            inline = cursor
        }
    }

    /**
     * Obtains coordinates pointing at the beginning of a specific line in the text.
     */
    public fun atLine(line: Int): TextCoordinates = textCoordinates {
        wholeLine = line
    }

    public companion object {

        /**
         * Obtains coordinates pointing at the beginning of the first line in the text.
         */
        @get:JvmName("startOfFile")
        @get:JvmStatic
        public val startOfFile: TextCoordinates = textCoordinates {
            wholeLine = 0
        }

        /**
         * Obtains coordinates pointing at the point after the last line in the text.
         */
        @get:JvmName("endOfFile")
        @get:JvmStatic
        public val endOfFile: TextCoordinates = textCoordinates {
            endOfText = Empty.getDefaultInstance()
        }

        /**
         * Obtains coordinates that do not point at anywhere in the text.
         *
         * The insertion point will NOT be placed in the file at question.
         */
        @get:JvmName("nowhere")
        @get:JvmStatic
        public val nowhere: TextCoordinates = textCoordinates {
            nowhere = Empty.getDefaultInstance()
        }
    }
}
