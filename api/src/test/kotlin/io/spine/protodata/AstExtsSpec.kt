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

package io.spine.protodata

import com.google.protobuf.Descriptors
import com.google.protobuf.Empty
import io.kotest.matchers.shouldBe
import io.spine.protodata.CallCardinality.BIDIRECTIONAL_STREAMING
import io.spine.protodata.CallCardinality.CLIENT_STREAMING
import io.spine.protodata.CallCardinality.SERVER_STREAMING
import io.spine.protodata.CallCardinality.UNARY
import io.spine.protodata.test.DoctorProto
import io.spine.protodata.test.TopLevelEnum
import io.spine.protodata.test.TopLevelMessage
import io.spine.protodata.test.TopLevelMessage.NestedEnum
import io.spine.protodata.test.TopLevelMessage.NestedMessage
import io.spine.protodata.test.packageless.GlobalMessage
import io.spine.protodata.test.packageless.GlobalMessage.LocalMessage
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
@DisplayName("AST extensions should")
class AstExtsSpec {

    @Nested inner class
    `Check if a field is` {

        @Test
        fun `repeated if list`() {
            val field = Field.newBuilder()
                .setList(Empty.getDefaultInstance())
                .buildPartial()

            field.isRepeated shouldBe true
        }

        @Test
        fun `repeated if map`() {
            val field = Field.newBuilder()
                .setMap(
                    Field.OfMap.newBuilder()
                    .setKeyType(PrimitiveType.TYPE_STRING)
                    .build())
                .buildPartial()

            field.isRepeated shouldBe true
        }

        @Test
        fun `not repeated`() {
            val field = Field.newBuilder()
                .setSingle(Empty.getDefaultInstance())
                .buildPartial()

            field.isRepeated shouldBe false
        }
    }

    @Nested inner class
    `Recognize RPC cardinality` {

        @Test
        fun unary() {
            val method = method("who")

            method.cardinality shouldBe UNARY
        }

        @Test
        fun `server streaming`() {
            val method = method("where_are_you")

            method.cardinality shouldBe SERVER_STREAMING
        }

        @Test
        fun `client streaming`() {
            val method = method("rescue_call")

            method.cardinality shouldBe CLIENT_STREAMING
        }

        @Test
        fun `bidirectional streaming`() {
            val method = method("which_actor")

            method.cardinality shouldBe BIDIRECTIONAL_STREAMING
        }

        private fun method(name: String): Descriptors.MethodDescriptor {
            val service = DoctorProto.getDescriptor().services[0]
            return service.methods.find { it.name == name }!!
        }
    }

    @Nested inner class
    `Obtain 'TypeName'` {

        @Test
        fun `for a top-level message`() {
            val name = TopLevelMessage.getDescriptor().name()

            name.qualifiedName shouldBe "spine.protodata.test.TopLevelMessage"
        }

        @Test
        fun `for a top-level enum`() {
            val name = TopLevelEnum.getDescriptor().name()

            name.qualifiedName shouldBe "spine.protodata.test.TopLevelEnum"
        }

        @Test
        fun `for a nested message`() {
            val name = NestedMessage.VeryNestedMessage.getDescriptor().name()

            name.qualifiedName shouldBe
                    "spine.protodata.test.TopLevelMessage.NestedMessage.VeryNestedMessage"
        }

        @Test
        fun `for a nested enum`() {
            val name = NestedEnum.getDescriptor().name()

            name.qualifiedName shouldBe "spine.protodata.test.TopLevelMessage.NestedEnum"
        }

        @Test
        fun `for a top-level message without a package`() {
            val name = GlobalMessage.getDescriptor().name()

            name.typeUrl shouldBe "type.googleapis.com/GlobalMessage"
        }

        @Test
        fun `for a nested message without a package`() {
            val name = LocalMessage.getDescriptor().name()

            name.qualifiedName shouldBe "GlobalMessage.LocalMessage"
        }
    }
}
