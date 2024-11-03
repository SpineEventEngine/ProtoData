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

package io.spine.protodata.ast

import io.kotest.matchers.shouldBe
import io.spine.protodata.ast.Cardinality.CARDINALITY_LIST
import io.spine.protodata.ast.Cardinality.CARDINALITY_MAP
import io.spine.protodata.ast.Cardinality.CARDINALITY_SINGLE
import io.spine.protodata.ast.FieldType.KindCase
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT64
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT64
import io.spine.protodata.protobuf.name
import io.spine.protodata.protobuf.toMessageType
import io.spine.protodata.protobuf.toPbSourceFile
import io.spine.protodata.protobuf.toType
import io.spine.protodata.type.TypeSystem
import io.spine.test.type.Anybody
import io.spine.test.type.Email
import io.spine.test.type.FieldSamples
import io.spine.test.type.FieldTypeSpecProto
import io.spine.test.type.Priority
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`FieldType` should")
internal class FieldTypeSpec {

    @Nested inner class
    `provide a name of a` : TypeTest() {

        /** Obtains a type name of the field with the given name. */
        private fun nameOf(fieldName: String) = typeOf(fieldName).name

        @Test
        fun `primitive field`() = nameOf("count") shouldBe "int32"

        @Test
        fun `message field`() = nameOf("email") shouldBe "given.type.Email"

        @Test
        fun `enum field`() = nameOf("assumed") shouldBe "given.type.Priority"

        @Test
        fun `repeated primitive field`() = nameOf("counts") shouldBe "repeated uint64"

        @Test
        fun `repeated message field`() = nameOf("emails") shouldBe "repeated given.type.Email"

        @Test
        fun `map with values of a primitive type`() =
            nameOf("histogram") shouldBe "map<string, sint64>"

        @Test
        fun `map with values of a message type`() =
            nameOf("sorted") shouldBe "map<int32, given.type.Email>"

        @Test
        fun `returning the name of 'kindCase' otherwise`() {
            FieldType.getDefaultInstance().name shouldBe KindCase.KIND_NOT_SET.name
        }
    }

    @Test
    fun `tell if it is a list`() {
        val messageType = FieldSamples.getDescriptor().toMessageType()
        fun isList(fieldName: String) = messageType.field(fieldName).type.isList

        isList("count") shouldBe false
        isList("counts") shouldBe true
        isList("names") shouldBe true
        isList("histogram") shouldBe false
    }

    @Test
    fun `tell if it is a map`() {
        val messageType = FieldSamples.getDescriptor().toMessageType()
        fun isMap(fieldName: String) = messageType.field(fieldName).type.isMap

        isMap("count") shouldBe false
        isMap("counts") shouldBe false
        isMap("names") shouldBe false
        isMap("histogram") shouldBe true
        isMap("sorted") shouldBe true
    }

    @Test
    fun `tell if it is singular`() {
        val messageType = FieldSamples.getDescriptor().toMessageType()
        fun isSingular(fieldName: String) = messageType.field(fieldName).type.isSingular

        isSingular("count") shouldBe true
        isSingular("name") shouldBe true

        isSingular("names") shouldBe false
        isSingular("priorities") shouldBe false
        isSingular("histogram") shouldBe false

        FieldType.getDefaultInstance().isSingular shouldBe false
    }

    @Nested inner class
    `obtain cardinality of` : TypeTest()  {

        private fun cardinalityOf(fieldName: String): Cardinality = typeOf(fieldName).cardinality

        @Test
        fun `single fields`() = cardinalityOf("count") shouldBe CARDINALITY_SINGLE

        @Test
        fun `list fields`() = cardinalityOf("counts") shouldBe CARDINALITY_LIST

        @Test
        fun `map fields`() = cardinalityOf("histogram") shouldBe CARDINALITY_MAP

        @Test
        fun `throwing when no type information available`() {
            assertThrows<IllegalStateException> {
                FieldType.getDefaultInstance().cardinality
            }
        }
    }

    @Nested inner class
    `convert itself to 'Type'` : TypeTest() {

        private fun toType(fieldName: String): Type = typeOf(fieldName).toType()

        @Test
        fun `when primitive`() {
            toType("count") shouldBe type { primitive = TYPE_INT32 }
            toType("name") shouldBe type { primitive = TYPE_STRING }
        }

        @Test
        fun `when message`() =
            toType("email") shouldBe type { message = Email.getDescriptor().name() }

        @Test
        fun `when enum`() =
            toType("assumed") shouldBe type { enumeration = Priority.getDescriptor().name() }

        @Test
        fun `rejecting when list`() {
            assertThrows<IllegalStateException> {
                toType("emails")
            }
        }

        @Test
        fun `rejecting when map`() {
            assertThrows<IllegalStateException> {
                toType("sorted")
            }
        }
    }

    @Nested inner class
    `extract 'MessageType'` : TypeTest() {

        private val typeSystem = TypeSystem(setOf(
            FieldTypeSpecProto.getDescriptor().toPbSourceFile()
        ))

        private fun messageTypeFrom(fieldName: String): MessageType? =
            typeOf(fieldName).extractMessageType(typeSystem)

        private val expected = Email.getDescriptor().toMessageType()

        @Test
        fun `when message`() = messageTypeFrom("email") shouldBe expected

        @Test
        fun `when list`() = messageTypeFrom("emails") shouldBe expected

        @Test
        fun `when map`() = messageTypeFrom("sorted") shouldBe expected

        @Test
        fun `returning 'null' for other types`() {
            messageTypeFrom("count") shouldBe null
            messageTypeFrom("names") shouldBe null
            messageTypeFrom("priorities") shouldBe null
            messageTypeFrom("histogram") shouldBe null
        }
    }

    @Nested inner class
    `extract 'TypeName'` : TypeTest() {

        private fun typeNameOf(fieldName: String): TypeName? = typeOf(fieldName).extractTypeName()

        private val expectedMessage = Email.getDescriptor().name()
        private val expectedEnum = Priority.getDescriptor().name()

        @Test
        fun `when refers to a message`() {
            typeNameOf("email") shouldBe expectedMessage
            typeNameOf("emails") shouldBe expectedMessage
            typeNameOf("sorted") shouldBe expectedMessage
        }

        @Test
        fun `when refers to an enum`() {
            typeNameOf("assumed") shouldBe expectedEnum
            typeNameOf("priorities") shouldBe expectedEnum
        }
        
        @Test
        fun `returning 'null' for other types`() {
            typeNameOf("count") shouldBe null
            typeNameOf("names") shouldBe null
            typeNameOf("names") shouldBe null
            typeNameOf("histogram") shouldBe null
        }
    }

    @Nested inner class
    `extract 'PrimitiveType'` : TypeTest() {

        private fun extractFrom(fieldName: String): PrimitiveType? =
            typeOf(fieldName).extractPrimitiveType()

        @Test
        fun `from singular`() = extractFrom("count") shouldBe TYPE_INT32

        @Test
        fun `from list`() = extractFrom("counts") shouldBe TYPE_UINT64

        @Test
        fun `from map`() = extractFrom("histogram") shouldBe TYPE_SINT64

        @Test
        fun `returning 'null' if does not refer to a primitive type`() {
            extractFrom("email") shouldBe null
            extractFrom("emails") shouldBe null
            extractFrom("assumed") shouldBe null
            // Check we do not extract from keys.
            extractFrom("sorted") shouldBe null
        }
    }

    @Nested inner class
    `extract 'Type'` : TypeTest() {

        private fun extractFrom(fieldName: String): Type = typeOf(fieldName).extractType()

        private val expectedMessageType = Email.getDescriptor().toType()

        @Test
        fun `when primitive`() = extractFrom("count") shouldBe TYPE_INT32.toType()

        @Test
        fun `when list of primitives`() = extractFrom("counts") shouldBe TYPE_UINT64.toType()

        @Test
        fun `when message`() = extractFrom("email") shouldBe expectedMessageType

        @Test
        fun `when message list`() = extractFrom("emails") shouldBe expectedMessageType

        @Test
        fun `when a map with messages`() = extractFrom("sorted") shouldBe expectedMessageType

        @Test
        fun `when a map with primitives`() = extractFrom("histogram") shouldBe TYPE_SINT64.toType()

        @Test
        fun `throwing when type info is not available`() {
            assertThrows<IllegalStateException> {
                FieldType.getDefaultInstance().extractType()
            }
        }
    }

    @Test
    fun `tell if refers to a message type`() {
        val messageType = FieldSamples.getDescriptor().toMessageType()
        fun refersToMessage(fieldName: String) = messageType.field(fieldName).type.refersToMessage()

        refersToMessage("email") shouldBe true
        refersToMessage("emails") shouldBe true
        refersToMessage("sorted") shouldBe true

        refersToMessage("assumed") shouldBe false
        refersToMessage("count") shouldBe false
    }

    @Test
    fun `tell if refers to 'Any'`() {
        val messageType = Anybody.getDescriptor().toMessageType()
        fun refersToAny(fieldName: String) = messageType.field(fieldName).type.refersToAny()

        refersToAny("home") shouldBe true
        refersToAny("neighbourhood") shouldBe true
        refersToAny("lottery") shouldBe true

        refersToAny("nobody") shouldBe false
        refersToAny("out_there") shouldBe false
        refersToAny("void") shouldBe false
        refersToAny("mockery") shouldBe false
    }
}

/**
 * Abstract base for nested test suites accessing field types of the given [messageType].
 */
abstract class TypeTest(
    private val messageType: MessageType = FieldSamples.getDescriptor().toMessageType()
) {
    protected fun typeOf(fieldName: String): FieldType = messageType.field(fieldName).type
}
