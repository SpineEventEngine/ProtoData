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

package io.spine.protodata.value

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.spine.base.fieldPath
import io.spine.option.MaxOption
import io.spine.option.MinOption
import io.spine.protodata.ast.find
import io.spine.protodata.given.value.DiceRoll
import io.spine.protodata.given.value.FieldOptionsProto
import io.spine.protodata.given.value.KelvinTemperature
import io.spine.protodata.given.value.Misreferences
import io.spine.protodata.given.value.NumberGenerated
import io.spine.protodata.given.value.Range
import io.spine.protodata.protobuf.toField
import io.spine.protodata.protobuf.toPbSourceFile
import io.spine.protodata.type.TypeSystem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Extensions for option types should")
internal class OptionsSpec {

    private val typeSystem: TypeSystem by lazy {
        val protoSources = setOf(
            FieldOptionsProto.getDescriptor()
        ).map { it.toPbSourceFile() }.toSet()
        TypeSystem(protoSources)
    }

    @Test
    fun `parse integer values`() {
        val field = DiceRoll.getDescriptor().fields[0].toField()
        val minOption = field.optionList.find<MinOption>()
        val maxOption = field.optionList.find<MaxOption>()

        minOption shouldNotBe null
        maxOption shouldNotBe null

        minOption!!.parse(field, typeSystem) shouldBe value {
            intValue = 1
        }
        maxOption!!.parse(field, typeSystem) shouldBe value {
            intValue = 6
        }
    }

    @Test
    fun `parse floating point values`() {
        val field = KelvinTemperature.getDescriptor().fields[0].toField()
        val option = field.optionList.find<MinOption>()

        option shouldNotBe null
        option!!.parse(field, typeSystem) shouldBe value {
            doubleValue = 0.0
        }
    }

    @Test
    fun `parse reference in the same type`() {
        val field = Range.getDescriptor().fields[0].toField()
        val option = field.optionList.find<MaxOption>()

        option shouldNotBe null
        option!!.parse(field, typeSystem) shouldBe value {
            reference = reference {
                type = field.type
                target = fieldPath {
                    fieldName.add("max_value")
                }
            }
        }
    }

    @Test
    fun `parse references to nested fields`() {
        val field = NumberGenerated.getDescriptor().fields[0].toField()
        val minOption = field.optionList.find<MinOption>()
        val maxOption = field.optionList.find<MaxOption>()

        minOption shouldNotBe null
        maxOption shouldNotBe null

        minOption!!.parse(field, typeSystem) shouldBe value {
            reference = reference {
                type = field.type
                target = fieldPath {
                    fieldName.add("range")
                    fieldName.add("min_value")
                }
            }
        }

        maxOption!!.parse(field, typeSystem) shouldBe value {
            reference = reference {
                type = field.type
                target = fieldPath {
                    fieldName.add("range")
                    fieldName.add("max_value")
                }
            }
        }
    }

    @Nested inner class
    `prohibit missing references` {

        @Test
        fun `of top level fields`() {
            val field = Misreferences.getDescriptor().fields[0].toField()
            val option = field.optionList.find<MinOption>()
            field.name.value shouldBe "wrong_direct"

            assertThrows<IllegalStateException> {
                option!!.parse(field, typeSystem)
            }
        }

        @Test
        fun `of top nested fields`() {
            val field = Misreferences.getDescriptor().fields[1].toField()
            val option = field.optionList.find<MaxOption>()
            field.name.value shouldBe "wrong_indirect"

            assertThrows<IllegalStateException> {
                option!!.parse(field, typeSystem)
            }
        }
    }

    @Test
    fun `require same field type for reference`() {
        val field = Misreferences.getDescriptor().fields[2].toField()
        field.name.value shouldBe "wrong_type"

        val option = field.optionList.find<MaxOption>()

        val e = assertThrows<IllegalStateException> {
            option!!.parse(field, typeSystem)
        }
        e.message.let {
            it shouldContain "(max).value"
            it shouldContain field.name.value
            it shouldContain "range.max_value"
            it shouldContain "int64"
            it shouldContain "int32"
        }
    }
}
