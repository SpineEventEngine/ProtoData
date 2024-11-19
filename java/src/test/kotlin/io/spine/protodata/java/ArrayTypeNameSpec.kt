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

package io.spine.protodata.java

import assertCode
import com.google.protobuf.Timestamp
import io.spine.protodata.java.JavaTypeName.BOOLEAN
import io.spine.protodata.java.JavaTypeName.BYTE
import io.spine.protodata.java.JavaTypeName.CHAR
import io.spine.protodata.java.JavaTypeName.DOUBLE
import io.spine.protodata.java.JavaTypeName.FLOAT
import io.spine.protodata.java.JavaTypeName.INT
import io.spine.protodata.java.JavaTypeName.LONG
import io.spine.protodata.java.JavaTypeName.SHORT
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ArrayTypeName` should print array")
internal class ArrayTypeNameSpec {

    @Test
    fun `of primitive types`() {
        assertArrayName(BYTE, "byte[]")
        assertArrayName(SHORT, "short[]")
        assertArrayName(INT, "int[]")
        assertArrayName(LONG, "long[]")
        assertArrayName(FLOAT, "float[]")
        assertArrayName(DOUBLE, "double[]")
        assertArrayName(BOOLEAN, "boolean[]")
        assertArrayName(CHAR, "char[]")
    }

    @Test
    fun `of classes`() {
        val string = ClassName(String::class)
        assertArrayName(string, "java.lang.String[]")
    }

    @Test
    fun `of generic variables`() {
        assertArrayName(TypeVariableName.T, "T[]")
        assertArrayName(TypeVariableName.E, "E[]")
    }

    @Test
    fun `of parameterized types`() {
        val timestamp = ClassName(Timestamp::class)
        val comparator = ClassName(Comparator::class)
        val timestampComparator = ParameterizedClassName(comparator, timestamp)
        assertArrayName(
            timestampComparator,
            "java.util.Comparator<com.google.protobuf.Timestamp>[]"
        )
    }

    @Test
    fun `of other arrays`() {
        val ints = ArrayTypeName(INT)
        assertArrayName(ints, "int[][]")
    }
}

private fun assertArrayName(type: JavaTypeName, expected: String) =
    assertCode(ArrayTypeName(type), expected)
