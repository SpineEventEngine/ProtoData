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

package io.spine.protodata.codegen.java

import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.spine.protobuf.TypeConverter
import io.spine.protodata.Field
import io.spine.protodata.Field.CardinalityCase.SINGLE
import io.spine.protodata.FieldName
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.test.Incarnation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Expression` should")
class ExpressionSpec {

    @Test
    fun `pack value into any`() {
        val expression = Literal("messageVar")
        val packed = expression.packToAny()
        assertCode(packed, "${TypeConverter::class.qualifiedName}.toAny(messageVar)")
    }

    @Nested
    inner class Print {

        @Test
        fun `Java string literal`() {
            val expression = LiteralString("foo")
            assertCode(expression, "\"foo\"")
        }

        @Test
        fun `ByteString constructor invocation`() {
            val bytes = "foobar".toByteArray()
            val expression = LiteralBytes(ByteString.copyFrom(bytes))
            assertCode(
                expression,
                "${ByteString::class.qualifiedName}.copyFrom(new byte[]{${bytes.joinToString()}})"
            )
        }

        @Test
        fun `literal 'null'`() {
            val expression = Null
            assertCode(expression, "null")
        }

        @Test
        fun `literal 'this'`() {
            val expression = This
            assertCode(expression, "this")
        }

        @Test
        fun `a number`() {
            val expression = Literal(42)
            assertCode(expression, "42")
        }

        @Test
        fun `a boolean`() {
            val expression = Literal(false)
            assertCode(expression, "false")
        }

        @Test
        fun anything() {
            val anything = "Frankie says relax"
            val expression = Literal(anything)
            assertCode(expression, anything)
        }
    }
}

class `'This' should` {

    @Test
    fun `convert to a 'MessageReference'`() {
        val msg = This.asMessage
        val field = msg.field("foo", SINGLE)
        field.getter.toCode() shouldBe "this.getFoo()"
    }
}

class `'MessageReference' should` {

    @Test
    fun `print name`() {
        val referenceName = "value"
        val messageReference = MessageReference(referenceName)
        assertCode(messageReference, referenceName)
    }

    @Test
    fun `access a singular field`() {
        val messageReference = MessageReference("msg")
        val field = Field
            .newBuilder()
            .setName(FieldName.newBuilder().setValue("baz"))
            .setSingle(Empty.getDefaultInstance())
            .build()
        val fieldAccess = messageReference.field(field)
        assertCode(fieldAccess.getter, "msg.getBaz()")
    }

    @Test
    fun `access a list field`() {
        val messageReference = MessageReference("msg")
        val field = Field
            .newBuilder()
            .setName(FieldName.newBuilder().setValue("baz"))
            .setList(Empty.getDefaultInstance())
            .build()
        val fieldAccess = messageReference.field(field)
        assertCode(fieldAccess.getter, "msg.getBazList()")
    }

    @Test
    fun `access a map field`() {
        val messageReference = MessageReference("msg")
        val field = Field
            .newBuilder()
            .setName(FieldName.newBuilder().setValue("baz"))
            .setMap(Field.OfMap.newBuilder().setKeyType(TYPE_STRING))
            .build()
        val fieldAccess = messageReference.field(field)
        assertCode(fieldAccess.getter, "msg.getBazMap()")
    }
}

class `'ClassName' should` {

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
        val className = ClassName(cls)
        assertCode(className.enumValue(2), "${cls.qualifiedName}.forNumber(2)")
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
}

private fun assertCode(expression: Expression, code: String) {
    expression.toCode() shouldBe code
}
