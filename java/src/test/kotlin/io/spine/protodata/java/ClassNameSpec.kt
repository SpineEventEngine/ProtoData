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

package io.spine.protodata.java

import assertCode
import com.google.protobuf.Timestamp
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.spine.protodata.test.Incarnation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`ClassName` should")
internal class ClassNameSpec {

    @Test
    fun `parse from a string`() {
        val className = ClassName("com.acme", "Cls")
        assertCode(className.getDefaultInstance(), "com.acme.Cls.getDefaultInstance()")
    }

    @Test
    fun `parse from a Java class`() {
        val cls = Timestamp::class.java
        val className = ClassName(cls)
        assertCode(className.getDefaultInstance(), "${cls.canonicalName}.getDefaultInstance()")
    }

    @Test
    fun `parse from a Kotlin class`() {
        val cls = Timestamp::class
        val className = ClassName(cls)
        assertCode(className.getDefaultInstance(), "${cls.qualifiedName}.getDefaultInstance()")
    }

    @Test
    fun `create new builder instances`() {
        val cls = Timestamp::class
        val className = ClassName(cls)
        assertCode(className.newBuilder(), "${cls.qualifiedName}.newBuilder()")
    }

    @Test
    fun `obtain enum constants by number`() {
        val cls = Incarnation::class
        val enumName = EnumName(cls)
        assertCode(enumName.enumValue(2), "${cls.qualifiedName}.forNumber(2)")
    }

    @Test
    fun `obtain canonical name`() {
        val cls = ClassName(Timestamp.Builder::class)
        cls.canonical shouldBe "com.google.protobuf.Timestamp.Builder"
    }

    @Test
    fun `obtain binary name`() {
        val cls = ClassName(Timestamp.Builder::class)
        cls.binary shouldBe "com.google.protobuf.Timestamp\$Builder"
    }

    @Test
    fun `obtain a nested name`() {
        ClassName(Timestamp::class).nested("Builder") shouldBe ClassName(Timestamp.Builder::class)
    }

    @Nested inner class
    `offer best guess` {

        @Test
        fun `for a qualified name`() {
            val className = ClassName.guess("com.example.My.Class")
            className.run {
                packageName shouldBe "com.example"
                simpleNames shouldContainExactly listOf("My", "Class")
            }
        }

        @Test
        fun `for a simple name`() {
            val className = ClassName.guess("MyClass")
            className.run {
                packageName shouldBe ""
                simpleNames shouldContainExactly listOf("MyClass")
                simpleName shouldBe "MyClass"
            }
        }

        @Test
        fun `prohibiting empty or blank names`() {
            assertThrows<IllegalArgumentException> { ClassName.guess("") }
            assertThrows<IllegalArgumentException> { ClassName.guess("  ") }
            assertThrows<IllegalArgumentException> { ClassName.guess("\n") }
        }

        @Test
        fun `prohibiting binary class name separate in the name`() {
            assertThrows<IllegalArgumentException> {
                ClassName.guess("org.example.Guess\$What")
            }
        }
    }

    @Test
    fun `prohibit package separators in simple names`() {
        assertThrows<IllegalArgumentException> {
            ClassName("org.example", listOf("The", "Middle.Class", "Is", ".Dead"))
        }
    }

    @Test
    fun `prohibit binary separators in simple names`() {
        assertThrows<IllegalArgumentException> {
            ClassName("org.example", listOf("The", "Middle\$Class", "Is", "\$Dead"))
        }
    }
}
