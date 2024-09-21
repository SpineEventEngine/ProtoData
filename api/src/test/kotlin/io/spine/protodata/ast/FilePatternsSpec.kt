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

import com.google.protobuf.Any
import com.google.protobuf.BytesValue
import com.google.protobuf.Empty
import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.spine.protodata.ast.FilePatternFactory.prefix
import io.spine.protodata.ast.FilePatternFactory.regex
import io.spine.protodata.ast.FilePatternFactory.suffix
import io.spine.validate.ValidationError
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("`FilePattern` extensions should")
internal class FilePatternsSpec {

    @Nested inner class
    `Prohibit empty or blank`{

        @Test
        fun suffix() {
            assertThrowing { suffix("") }
            assertThrowing { suffix(" ") }
        }

        @Test
        fun prefix() {
            assertThrowing { prefix("") }
            assertThrowing { prefix(" ") }
        }

        @Test
        fun regex() {
            assertThrowing { regex("") }
            assertThrowing { regex(" ") }
        }

        private fun assertThrowing(call: () -> FilePattern) {
            assertThrows<IllegalArgumentException> {
                call.invoke()
            }
        }
    }

    @Nested inner class
    `Match a 'MessageType' file by` {

        @Test
        fun prefix() {
            prefix("google/protobuf/any").run {
                matches(messageTypeOf<Any>()) shouldBe true
                matches(messageTypeOf<Timestamp>()) shouldBe false
            }
        }

        @Test
        fun suffix() {
            suffix("y.proto").run {
                matches(messageTypeOf<Any>()) shouldBe true
                matches(messageTypeOf<Empty>()) shouldBe true
                matches(messageTypeOf<BytesValue>()) shouldBe false
            }
        }

        @Test
        fun regex() {
            val protobufFile = "google/protobuf/([a-z0-9_-]+)\\.proto\$"
            // Smoke test for the regex itself.
            val regex = Regex(protobufFile)
            regex.matches("google/protobuf/any.proto") shouldBe true

            regex(protobufFile).run {
                matches(messageTypeOf<Any>()) shouldBe true
                matches(messageTypeOf<Empty>()) shouldBe true
                matches(messageTypeOf<BytesValue>()) shouldBe true
                matches(messageTypeOf<ValidationError>()) shouldBe false
            }
        }
    }

    @Nested inner class
    `Match a file by` {

        @Test
        fun prefix() {
            prefix("C:/").run {
                matches(file { path = "C:/"}) shouldBe true
                matches(file { path = "C:/autoexec.bat" }) shouldBe true
                matches(file { path = "custom.proto" }) shouldBe false
            }
        }

        @Test
        fun suffix() {
            suffix("y.proto").run {
                matches(file { path = "any.proto" }) shouldBe true
                matches(file { path = "protobuf/empty.proto" }) shouldBe true
                matches(file { path = "api.proto" }) shouldBe false
            }
        }
    }
}
