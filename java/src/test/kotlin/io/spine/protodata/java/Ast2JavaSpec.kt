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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Struct
import com.google.protobuf.Value
import io.kotest.matchers.shouldBe
import io.spine.protobuf.field
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.fieldName
import io.spine.protodata.protobuf.toField
import io.spine.protodata.value.ListValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Ast2Java` should teach")
internal class Ast2JavaSpec {

    @Nested
    inner class
    `'FieldName' class` {

        @Test
        fun `providing Java version of the name`() {
            javaCaseOf("foo_bar") shouldBe "fooBar"
            javaCaseOf("FizBuz") shouldBe "fizBuz"
            // No splitting by digits, just lowercase first char.
            javaCaseOf("Funny2do") shouldBe "funny2do"
        }

        private fun javaCaseOf(fieldName: String) =
            fieldName { value = fieldName }.javaCase()
    }

    @Nested
    inner class
    `'Field' class providing` {

        private val valueDescriptor = Value.getDescriptor()

        @Nested inner class
        `primary setter name for` {

            @Test
            fun `regular type`() {
                valueDescriptor.setterFor("number_value") shouldBe "setNumberValue"
            }

            @Test
            fun `map type`() {
                Struct.getDescriptor().setterFor("fields") shouldBe "putAllFields"
            }

            @Test
            fun `repeated type`() {
                ListValue.getDescriptor().setterFor("values") shouldBe "addAllValues"
            }

            private fun Descriptor.setterFor(fieldName: String): String {
                val fld = field(fieldName)!!
                return fld.toField().primarySetterName
            }
        }

        @Nested inner class
        `getter name for` {

            private val valueDescriptor = Value.getDescriptor()

            @Test
            fun `regular type`() {
                valueDescriptor.getterFor("number_value") shouldBe "getNumberValue"
            }

            @Test
            fun `map type`() {
                Struct.getDescriptor().getterFor("fields") shouldBe "getFieldsMap"
            }

            @Test
            fun `repeated type`() {
                ListValue.getDescriptor().getterFor("values") shouldBe "getValuesList"
            }

            private fun Descriptor.getterFor(fieldName: String): String {
                val fld = field(fieldName)!!
                return fld.toField().getterName
            }
        }

        @Test
        fun `telling if it is of Java primitive type`() {
            valueField("string_value").isJavaPrimitive shouldBe false
            valueField("list_value").isJavaPrimitive shouldBe false

            valueField("number_value").isJavaPrimitive shouldBe true
            valueField("bool_value").isJavaPrimitive shouldBe true
        }

        private fun valueField(name: String): Field = valueDescriptor.field(name)!!.toField()
    }
}
