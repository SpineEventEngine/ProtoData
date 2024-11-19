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

import com.google.protobuf.Timestamp
import com.google.protobuf.TimestampProto
import io.kotest.matchers.shouldBe
import io.spine.protodata.ast.FieldType
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.field
import io.spine.protodata.protobuf.toMessageType
import io.spine.protodata.protobuf.toPbSourceFile
import io.spine.protodata.type.TypeSystem
import io.spine.test.fields.FieldExamples
import io.spine.test.fields.FieldExtsSpecProto
import io.spine.test.fields.Flower
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`FieldType` extensions should")
internal class FieldTypeExtsSpec {

    @Nested
    inner class
    `provide Java class name for` : FieldTypeTest() {

        @Test
        fun `a message field`() {
            typeOf("timestamp").javaClassName(typeSystem) shouldBe ClassName(Timestamp::class.java)
        }

        @Test
        fun `an enum field`() {
            typeOf("flower").javaClassName(typeSystem) shouldBe ClassName(Flower::class.java)
        }

        @Test
        fun `a primitive field`() {
            typeOf("total").javaClassName(typeSystem) shouldBe ClassName(Integer::class.java)
        }

        @Test
        fun `a repeated field`() {
            typeOf("note").javaClassName(typeSystem) shouldBe ClassName(List::class.java)
        }

        @Test
        fun `a map field`() {
            typeOf("count").javaClassName(typeSystem) shouldBe ClassName(Map::class.java)
        }
    }
}

abstract class FieldTypeTest(
    private val messageType: MessageType = FieldExamples.getDescriptor().toMessageType()
) {
    protected val typeSystem: TypeSystem by lazy {
        TypeSystem(setOf(
            FieldExtsSpecProto.getDescriptor().toPbSourceFile(),
            TimestampProto.getDescriptor().toPbSourceFile()
        ))
    }
    protected fun typeOf(fieldName: String): FieldType = messageType.field(fieldName).type
}
