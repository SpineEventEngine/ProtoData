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

import com.google.protobuf.Value
import io.kotest.matchers.shouldBe
import io.spine.base.ListOfAnys
import io.spine.protodata.protobuf.field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`Type` extensions should")
internal class TypeExtsSpec {

    @Nested inner class
    `Obtain simple type name` {

        private val messageType: Type
        private val enumType: Type
        private val primitiveType: Type
        private val mapType: Type
        private val repeatedType: Type

        init {
            val msg = io.spine.base.Error.getDescriptor()
            messageType = msg.field("details").type
            enumType = Value.getDescriptor().field("null_value").type
            primitiveType = msg.field("code").type
            mapType = msg.field("attributes").type
            repeatedType = ListOfAnys.getDescriptor().field("value").type
        }

        @Test
        fun `of 'Message' type`() {
            messageType.simpleName shouldBe "Any"
        }

        @Test
        fun `of enum type`() {
            enumType.simpleName shouldBe "NullValue"
        }

        @Test
        fun `rejecting for primitive types`() {
            assertThrows<IllegalStateException> {
                primitiveType.simpleName
            }
        }
    }
}
