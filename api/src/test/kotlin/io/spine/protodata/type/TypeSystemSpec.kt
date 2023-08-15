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

package io.spine.protodata.type

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.spine.protodata.EnumType
import io.spine.protodata.MessageType
import io.spine.protodata.test.TypesTestEnv.enumTypeName
import io.spine.protodata.test.TypesTestEnv.messageTypeName
import io.spine.protodata.test.TypesTestEnv.protoFile
import io.spine.protodata.test.TypesTestEnv.typeSystem
import io.spine.protodata.typeName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`TypeSystem` should")
class TypeSystemSpec {

    @Nested inner class
    find {
        @Test
        fun `message by name`() {
            val (messageType, file) = typeSystem.findMessage(messageTypeName)!!
            messageType shouldNotBe null
            file shouldNotBe null

            messageType.name shouldBe messageTypeName
            file shouldBe protoFile
        }

        @Test
        fun `enum by name`() {
            val (enumType, file) = typeSystem.findEnum(enumTypeName)!!
            enumType shouldNotBe null
            file shouldNotBe null

            enumType.name shouldBe enumTypeName
            file shouldBe protoFile
        }

        @Test
        fun `any type by name`() {
            val (enumType, _) = typeSystem.findMessageOrEnum(enumTypeName)!!
            enumType.shouldBeInstanceOf<EnumType>()

            val (messageType, _) = typeSystem.findMessageOrEnum(messageTypeName)!!
            messageType.shouldBeInstanceOf<MessageType>()
        }
    }

    @Nested inner class
    `not find` {

        @Test
        fun `message type by enum name`() {
            val declaration = typeSystem.findMessage(enumTypeName)
            declaration shouldBe null
        }

        @Test
        fun `enum type by message name`() {
            val declaration = typeSystem.findEnum(messageTypeName)
            declaration shouldBe null
        }

        @Test
        fun `unknown type`() {
            val declaration = typeSystem.findMessageOrEnum(typeName {
                simpleName = "ThisTypeIsUnknown"
                packageName = "com.acme"
            })
            declaration shouldBe null
        }
    }
}
