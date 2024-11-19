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
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import io.spine.protobuf.TypeConverter
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.TypeInstances
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.fieldName
import io.spine.protodata.ast.fieldType
import io.spine.protodata.ast.mapEntryType
import io.spine.protodata.ast.toFieldType
import io.spine.protodata.ast.typeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`ProtobufExpressions` should")
internal class ProtobufExpressionsSpec {

    private val message = ReadVar<Message>("messageVar")

    @Test
    fun `pack value into 'Any'`() {
        val packed = message.packToAny()
        assertCode(packed, "${TypeConverter::class.qualifiedName}.toAny(messageVar)")
    }

    @Test
    fun `yield the given 'ByteString' using 'copyFrom()'`() {
        val bytes = "foobar".toByteArray()
        val expression = CopyByteString(ByteString.copyFrom(bytes))
        assertCode(
            expression,
            "${ByteString::class.qualifiedName}.copyFrom(new byte[]{${bytes.joinToString()}})"
        )
    }

    @Nested inner class
    `for message expressions` {

        private val fieldName: FieldName = fieldName { value = "baz" }
        private val typeName: TypeName = typeName {
            simpleName = "StubType"
            packageName = "given.message"
        }

        @Test
        fun `access a singular field`() {
            val field = io.spine.protodata.ast.field {
                name = fieldName
                type = TypeInstances.string.toFieldType()
                declaringType = typeName
            }
            val fieldAccess = message.field(field)
            assertCode(fieldAccess.getter<Any>(), "$message.getBaz()")
        }

        @Test
        fun `access a list field`() {
            val field = io.spine.protodata.ast.field {
                name = fieldName
                type = fieldType { list = TypeInstances.string }
                declaringType = typeName
            }
            val fieldAccess = message.field(field)
            assertCode(fieldAccess.getter<Any>(), "$message.getBazList()")
        }

        @Test
        fun `access a map field`() {
            val field = io.spine.protodata.ast.field {
                name = fieldName
                type = fieldType {
                    map = mapEntryType {
                        keyType = PrimitiveType.TYPE_STRING
                        valueType = TypeInstances.string
                    }
                }
                declaringType = typeName
            }
            val fieldAccess = message.field(field)
            assertCode(fieldAccess.getter<Any>(), "$message.getBazMap()")
        }
    }
}
