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

package io.spine.protodata.codegen.java

import com.google.protobuf.ByteString
import com.google.protobuf.ByteString.copyFrom
import io.kotest.matchers.shouldBe
import io.spine.protodata.NullValue.NULL_VALUE
import io.spine.protodata.Value
import io.spine.protodata.test.TypesTestEnv.enumTypeName
import io.spine.protodata.test.TypesTestEnv.messageTypeName
import io.spine.protodata.test.TypesTestEnv.typeSystem
import io.spine.protodata.enumValue
import io.spine.protodata.messageValue
import io.spine.protodata.value
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`JavaValueConverter` should convert values into")
internal class JavaValueConverterSpec {

    private val converter = JavaValueConverter(MessageOrEnumConvention(typeSystem))

    @Test
    fun nulls() {
        val value = value { nullValue = NULL_VALUE }
        checkCode(value, "null")
    }

    @Test
    fun ints() {
        val value = value { intValue = 42 }
        checkCode(value, "42")
    }

    @Test
    fun floats() {
        val value = value { doubleValue = .1 }
        checkCode(value, "0.1")
    }

    @Test
    fun bool() {
        val value = value { boolValue = true }
        checkCode(value, "true")
    }

    @Test
    fun string() {
        val value = value { stringValue = "hello" }
        checkCode(value, "\"hello\"")
    }

    @Test
    fun bytes() {
        val value = value {
            bytesValue = copyFrom(ByteArray(3) { index -> index.toByte() })
        }
        checkCode(value, "${ByteString::class.qualifiedName}.copyFrom(new byte[]{0, 1, 2})")
    }

    @Test
    fun `empty message`() {
        val emptyMessage = messageValue { type = messageTypeName }
        val value = value { messageValue = emptyMessage }
        checkCode(value, "dev.acme.example.Foo.getDefaultInstance()")
    }

    @Test
    fun `message with a field`() {
        val message = messageValue {
            type = messageTypeName
            fields.put("bar", value { stringValue = "hello there" })
        }
        val value = value { messageValue = message }
        checkCode(value, "dev.acme.example.Foo.newBuilder().setBar(\"hello there\").build()")
    }

    @Test
    fun `enum value`() {
        val enumVal = enumValue {
            type = enumTypeName
            constNumber = 1
        }
        val value = value {
            enumValue = enumVal
        }
        checkCode(value, "dev.acme.example.Kind.forNumber(1)")
    }

    private fun checkCode(value: Value, expectedCode: String) {
        val expression = converter.valueToCode(value)
        expression.toCode() shouldBe expectedCode
    }
}
