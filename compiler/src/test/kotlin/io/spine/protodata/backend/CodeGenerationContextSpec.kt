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
import com.google.protobuf.StringValue
import com.google.protobuf.TimestampProto
import com.google.protobuf.WrappersProto
import com.google.protobuf.compiler.codeGeneratorRequest
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.option.OptionsProto
import io.spine.option.OptionsProto.BETA_TYPE_FIELD_NUMBER
import io.spine.protobuf.AnyPacker
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.ProtobufDependency
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.asType
import io.spine.protodata.backend.event.CompilerEvents
import io.spine.protodata.backend.event.toFilePath
import io.spine.protodata.option
import io.spine.protodata.path
import io.spine.protodata.test.DoctorProto
import io.spine.protodata.test.PhDProto
import io.spine.protodata.test.XtraOptsProto
import io.spine.server.BoundedContext
import io.spine.testing.server.blackbox.BlackBox
import io.spine.testing.server.blackbox.assertEntity
import io.spine.time.TimeProto
import io.spine.util.theOnly
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Code Generation` context should")
class CodeGenerationContextSpec {

    @Nested
    @DisplayName("provide")
    inner class ViewTypes {

        private lateinit var ctx: BoundedContext

        @BeforeEach
        fun setUp() {
            ctx = CodeGenerationContext.newInstance()
        }

        @AfterEach
        fun closeContext() {
            ctx.close()
        }

        @Test
        fun `'ProtobufSourceFile' view`() = assertTrue(
            ctx.hasEntitiesOfType(ProtoSourceFileView::class.java)
        )

        @Test
        fun `'ProtobufDependency' file view`() = assertTrue(
            ctx.hasEntitiesOfType(DependencyView::class.java)
        )
    }

    companion object Fixtures {

        val dependencies = listOf(
            AnyProto.getDescriptor(),
            DescriptorProtos.getDescriptor(),
            DoctorProto.getDescriptor(),
            EmptyProto.getDescriptor(),
            OptionsProto.getDescriptor(),
            PhDProto.getDescriptor(),
            TimestampProto.getDescriptor(),
            TimeProto.getDescriptor(),
            WrappersProto.getDescriptor(),
            XtraOptsProto.getDescriptor(),
        ).map { it.toProto() }

        val filesToGenerate = setOf(
            DoctorProto.getDescriptor().name,
            PhDProto.getDescriptor().name
        )

        private val codeGeneratorRequest = codeGeneratorRequest {
            protoFile.addAll(dependencies)
            fileToGenerate.addAll(filesToGenerate)
        }

        fun createCodegenBlackBox(): BlackBox = BlackBox.from(CodeGenerationContext.builder())

        fun emitCompilerEvents() {
            val events = CompilerEvents.parse(codeGeneratorRequest)
            ProtobufCompilerContext().use {
                it.emitted(events)
            }
        }
    }

    @Nested
    inner class `construct views based on a descriptor set` {

        private lateinit var ctx: BlackBox

        @BeforeEach
        fun buildViews() {
            ctx = createCodegenBlackBox()
            emitCompilerEvents()
        }

        @AfterEach
        fun closeContext() {
            ctx.close()
        }

        @Test
        fun `with files marked for generation`() {
            val assertSourceFile = ctx.assertEntity<ProtoSourceFileView, _>(
                DoctorProto.getDescriptor().path()
            )
            assertSourceFile.exists()

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
            assertSourceFile.exists()
        }

        @Test
        fun `with respect for custom options among the source files`() {
            val phdFile = ctx.assertEntity<ProtoSourceFileView, _>(
                PhDProto.getDescriptor().path()
            ).actual()!!.state() as ProtobufSourceFile
            val paperType = phdFile.typeMap.values.find { it.name.simpleName == "Paper" }!!
            val keywordsField = paperType.fieldList.find { it.name.value == "keywords" }!!
            keywordsField.optionList shouldHaveSize 1
            val option = keywordsField.optionList.theOnly()
            option.name shouldBe "xtra_option"
            option.value.unpack(StringValue::class.java).value shouldContain "please"
        }
    }
    
    @Nested
    inner class `construct views` {

        private val thirdPartyFiles = dependencies.filter { it.name !in filesToGenerate }

        private lateinit var dependencyFiles: List<ProtobufSourceFile>
        private lateinit var protoSourceFiles: List<ProtobufSourceFile>

        @BeforeEach
        fun buildViews() {
            // First context
            createCodegenBlackBox().run {
                use {
                    emitCompilerEvents()
                    dependencyFiles = thirdPartyFiles.map {
                        assertEntity<DependencyView, _>(
                            it.toFilePath()
                        ).run {
                            exists()
                            actual()!!.state()
                        }
                    }.map { state -> (state as ProtobufDependency).file }
                }
            }

            // Second context
            createCodegenBlackBox().run {
                use {
                    emitCompilerEventsForDependencyFiles()

                    protoSourceFiles = thirdPartyFiles.map {
                        assertEntity<ProtoSourceFileView, _>(
                            it.toFilePath()
                        ).run {
                            exists()
                            actual()!!.state() as ProtobufSourceFile
                        }
                    }
                }
            }
        }

        private fun emitCompilerEventsForDependencyFiles() {
            val set = codeGeneratorRequest {
                protoFile.addAll(dependencies)
                fileToGenerate.addAll(dependencies.map { it.name })
            }
            val events = CompilerEvents.parse(set)

            val eventList = events.toList()
            eventList.distinct() shouldContainExactly eventList

            ProtobufCompilerContext().use {
                it.emitted(events)
            }
        }

        @Test
        fun `exactly the same way for dependencies and for files to generate`() {
            assertThat(dependencyFiles)
                .ignoringRepeatedFieldOrder()
                .containsExactlyElementsIn(protoSourceFiles)
        }
    }
}
