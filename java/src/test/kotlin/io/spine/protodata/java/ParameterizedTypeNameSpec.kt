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
import io.spine.protodata.java.TypeVariableName.Companion.E
import io.spine.protodata.java.TypeVariableName.Companion.T
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`ParameterizedTypeName` should")
internal class ParameterizedTypeNameSpec {

    private val list = ClassName(List::class)

    @Nested
    inner class Accept {

        private val string = ClassName(String::class)
        private val listOfStrings = ParameterizedTypeName(list, string)

        @Test
        fun `a class name as a parameter`() =
            assertCode(listOfStrings, "java.util.List<java.lang.String>")

        @Test
        fun `generic variables as parameters`() {
            val map = ClassName(Map::class)
            val genericMap = ParameterizedTypeName(map, T, E)
            assertCode(genericMap, "java.util.Map<T, E>")
        }

        @Test
        fun `other parameterized classes`() {
            val comparator = ClassName(Comparator::class)
            val comparatorOfLists = ParameterizedTypeName(comparator, listOfStrings)
            assertCode(comparatorOfLists, "java.util.Comparator<java.util.List<java.lang.String>>")
        }

        @Test
        fun `an arbitrary Java type name`() {
            val arbitraryType = "C super java.util.Collection"
            val typeName = object : JavaTypeName() {
                override val canonical: String = arbitraryType

            }
            val comparatorOfLists = ParameterizedTypeName(list, typeName)
            assertCode(comparatorOfLists, "java.util.List<C super java.util.Collection>")
        }
    }

    @Nested
    inner class Throw {

        @Test
        fun `throw if not given any parameters`() {
            assertThrows<IllegalArgumentException> {
                ParameterizedTypeName(list, emptyList())
            }
        }

        @Test
        fun `throw if given a primitive type as a parameter`() {
            JavaTypeName.KnownPrimitives.forEach { primitive ->
                assertThrows<IllegalArgumentException> {
                    ParameterizedTypeName(list, primitive)
                }
            }
        }
    }
}
