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
import io.kotest.matchers.shouldBe
import io.spine.protodata.CallCardinality.BIDIRECTIONAL_STREAMING
import io.spine.protodata.CallCardinality.CLIENT_STREAMING
import io.spine.protodata.CallCardinality.SERVER_STREAMING
import io.spine.protodata.CallCardinality.UNARY
import io.spine.protodata.test.DoctorProto
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
@DisplayName("`MethodDescriptor` extensions should")
internal class MethodDescriptorExtsSpec {

    @Nested
    inner class
    `Recognize RPC cardinality` {

        @Test
        fun unary() {
            val method = method("Who")
            method.cardinality shouldBe UNARY
        }

        @Test
        fun `server streaming`() {
            val method = method("WhereAreYou")
            method.cardinality shouldBe SERVER_STREAMING
        }

        @Test
        fun `client streaming`() {
            val method = method("RescueCall")
            method.cardinality shouldBe CLIENT_STREAMING
        }

        @Test
        fun `bidirectional streaming`() {
            val method = method("WhichActor")
            method.cardinality shouldBe BIDIRECTIONAL_STREAMING
        }

        private fun method(name: String): Descriptors.MethodDescriptor {
            val service = DoctorProto.getDescriptor().services[0]
            return service.methods.find { it.name == name }!!
        }
    }
}
