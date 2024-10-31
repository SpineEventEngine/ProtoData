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
import io.spine.protodata.protobuf.toMessageType
import io.spine.test.type.OopFun
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`FieldType` extensions should")
internal class FieldTypeExtsSpec {

    @Nested inner class
    `obtain cardinality for`  {

        val type = OopFun.getDescriptor().toMessageType()

        @Test
        fun `map fields`() {
            cardinalityOf("gorillas") shouldBe CARDINALITY_MAP
        }

        @Test
        fun `list fields`() {
            cardinalityOf("tree") shouldBe CARDINALITY_LIST
        }

        @Test
        fun `single fields`() {
            cardinalityOf("jungle") shouldBe CARDINALITY_SINGLE
        }

        private fun cardinalityOf(fieldName: String): Cardinality =
            type.field(fieldName).type.cardinality
    }
}