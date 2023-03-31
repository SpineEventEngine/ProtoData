/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.backend

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.AnyProto
import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.EmptyProto
import com.google.protobuf.TimestampProto
import com.google.protobuf.WrappersProto
import com.google.protobuf.compiler.codeGeneratorRequest
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.spine.option.OptionsProto
import io.spine.option.OptionsProto.BETA_TYPE_FIELD_NUMBER
import io.spine.protobuf.AnyPacker
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.ProtobufDependency
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.asType
import io.spine.protodata.backend.event.CompilerEvents
import io.spine.protodata.filePath
import io.spine.protodata.option
import io.spine.protodata.path
import io.spine.protodata.test.DoctorProto
import io.spine.testing.server.blackbox.BlackBox
import io.spine.time.TimeProto
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Code Generation` context should")
class CodeGenerationContextSpec {

    @Test
    fun `contain 'ProtobufSourceFile' view`() {
        val ctx = CodeGenerationContext.builder().build()
        assertTrue(ctx.hasEntitiesOfType(ProtoSourceFileView::class.java))
    }

    @Test
    fun `contain 'ProtobufDependency' file view`() {
        val ctx = CodeGenerationContext.builder().build()
        assertTrue(ctx.hasEntitiesOfType(DependencyView::class.java))
    }

    @Nested
    inner class `construct views based on a descriptor set` {

        private lateinit var ctx: BlackBox
        private val dependencies = listOf(
            AnyProto.getDescriptor(),
            DescriptorProtos.getDescriptor(),
            DoctorProto.getDescriptor(),
            EmptyProto.getDescriptor(),
            OptionsProto.getDescriptor(),
            TimestampProto.getDescriptor(),
            TimeProto.getDescriptor(),
            WrappersProto.getDescriptor()
        ).map { it.toProto() }

        @BeforeEach
        fun buildViews() {
            ctx = BlackBox.from(CodeGenerationContext.builder())
            val set = codeGeneratorRequest {
                protoFile.addAll(dependencies)
                fileToGenerate.add(DoctorProto.getDescriptor().toProto().name)
            }
            ProtobufCompilerContext().use {
                it.emitted(CompilerEvents.parse(set))
            }
        }

        @Test
        fun `with files marked for generation`() {
            val assertSourceFile = ctx.assertEntity(
                DoctorProto.getDescriptor().path(), ProtoSourceFileView::class.java
            )
            assertSourceFile
                .exists()
            val actual = assertSourceFile.actual()!!.state() as ProtobufSourceFile

            val types = actual.typeMap
            val typeName = "type.spine.io/spine.protodata.test.Journey"
            types shouldContainKey typeName
            val journeyType = types[typeName]!!
            journeyType.name.typeUrl shouldBe typeName
            journeyType.optionList should containExactly(option {
                name = "beta_type"
                number = BETA_TYPE_FIELD_NUMBER
                type = TYPE_BOOL.asType()
                value = AnyPacker.pack(BoolValue.of(true))
            })
            journeyType.fieldList shouldHaveSize 4
            journeyType.oneofGroupList shouldHaveSize 1
            journeyType.oneofGroupList[0].fieldList shouldHaveSize 2
        }

        @Test
        fun `with dependencies`() {
            val assertSourceFile = ctx.assertEntity(
                AnyProto.getDescriptor().path(),
                DependencyView::class.java
            )
            assertSourceFile
                .exists()
        }

        @Test
        fun `exactly the same way for dependencies and for files to generate`() {
            val secondContext = BlackBox.from(CodeGenerationContext.builder())
            val set = codeGeneratorRequest {
                protoFile.addAll(dependencies)
                fileToGenerate.addAll(dependencies.map { it.name })
            }
            ProtobufCompilerContext().use {
                it.emitted(CompilerEvents.parse(set))
            }

            val recordsOfDependencies = dependencies
                .filter { it.name != DoctorProto.getDescriptor().name }
                .map {
                println(it.name)
                val assertEntity = ctx.assertEntity(
                    filePath { value = it.name },
                    DependencyView::class.java
                )
                assertEntity.exists()
                assertEntity.actual()!!.state()
            }.map { state -> (state as ProtobufDependency).file }
            val recordsOfFilesToGenerate = dependencies
                .filter { it.name != DoctorProto.getDescriptor().name }
                .map {
                    val assertEntity = secondContext.assertEntity(
                        filePath { value = it.name },
                        ProtoSourceFileView::class.java
                    )
                    assertEntity.exists()
                    assertEntity
                        .actual()!!.state() as ProtobufSourceFile
                }
            assertThat(recordsOfDependencies)
                .ignoringRepeatedFieldOrder()
                .containsExactlyElementsIn(recordsOfFilesToGenerate)
        }
    }
}
