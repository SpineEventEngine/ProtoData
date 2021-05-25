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

/**
 * A point is a source file, where more code may be inserted.
 */
public interface InsertionPoint {

    /**
     * The name of this insertion point.
     */
    public val label: String

    /**
     * Locates the line number where the insertion point should be added.
     *
     * An insertion point should only appear once in a file.
     *
     * @param
     *     lines a list of code lines in a source file
     * @return the line number at which the insertion point should be added.
     * @see LineNumber
     */
    public fun locate(lines: List<String>): LineNumber
}

/**
 * The marker representing this [InsertionPoint] in the code.
 *
 * This property is an extension rather than a normal property because we don't want users
 * to override it.
 */
public val InsertionPoint.codeLine: String
    get() = "INSERT:'${label}'"

/**
 * A pointer to a line in a source file.
 */
public sealed class LineNumber {

    public companion object {

        /**
         * Creates a `LineNumber` pointing at a given line.
         */
        @JvmStatic
        public fun at(number: Int): LineNumber = LineIndex(number)

        /**
         * Creates a `LineNumber` pointing at the start of the file.
         *
         * This is a convenience method equivalent to calling `at(0)`.
         */
        @JvmStatic
        public fun startOfFile(): LineNumber = at(0)

        /**
         * Creates a `LineNumber` pointing at the end of the file, no matter the index of the actual
         * line.
         */
        @JvmStatic
        public fun endOfFile(): LineNumber = EndOfFile

        /**
         * Creates a `LineNumber` not pointing at any line.
         */
        @JvmStatic
        public fun notInFile(): LineNumber = Nowhere
    }
}

/**
 * A [LineNumber] pointing at a particular line.
 *
 * Implementation note. We do not use unsigned integers here by design. `UInt`s in Kotlin are
 * designed to only provide the whole bit range, not to insure invariants.
 * See [this thread](https://youtrack.jetbrains.com/issue/KT-46144) for more details.
 */
internal data class LineIndex constructor(val value: Int) : LineNumber() {
    init {
        if (value < 0) {
            throw IndexOutOfBoundsException("Invalid line number: `$value`.")
        }
    }
}

/**
 * A [LineNumber] which always lies at the end of the file.
 */
internal object EndOfFile : LineNumber()

/**
 * A [LineNumber] representing that the looked up line is nowhere to be found in the file.
 */
internal object Nowhere : LineNumber()
