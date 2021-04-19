/*
 * Copyright 2021, TeamDev. All rights reserved.
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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS_VALUE
import com.google.protobuf.EnumValue
import com.google.protobuf.StringValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.base.EventMessage
import io.spine.option.OptionsProto.REQUIRED_FIELD_NUMBER
import io.spine.option.OptionsProto.TYPE_URL_PREFIX_FIELD_NUMBER
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protodata.test.DoctorProto
import io.spine.testing.Correspondences.type
import io.spine.type.KnownTypes
import kotlin.reflect.KClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class `'CompilerEvents' should` {

    private lateinit var events: List<EventMessage>

    @BeforeEach
    fun parseEvents() {
        val allTheTypes = KnownTypes.instance()
                                    .asTypeSet()
                                    .messageTypes()
                                    .map { it.descriptor().file.toProto() }
        val request = CodeGeneratorRequest
            .newBuilder()
            .addFileToGenerate(DoctorProto.getDescriptor().fullName)
            .addAllProtoFile(allTheTypes)
            .build()
        events = CompilerEvents.parse(request).toList()
    }

    @Test
    fun `produce file events`() {
        assertEmits(
            FileEntered::class,
            FileOptionDiscovered::class,
            FileOptionDiscovered::class,
            FileExited::class
        )
    }

    @Test
    fun `produce standard file option events`() {
        val event = events.find {
            it is FileOptionDiscovered && it.option.number == JAVA_MULTIPLE_FILES_FIELD_NUMBER
        } as FileOptionDiscovered?
        assertThat(event)
            .isNotNull()
        assertThat(event!!.option.value.unpack(BoolValue::class.java))
            .isEqualTo(BoolValue.of(true))
    }

    @Test
    fun `produce custom file option events`() {
        val event = events.find {
            it is FileOptionDiscovered && it.option.number == TYPE_URL_PREFIX_FIELD_NUMBER
        } as FileOptionDiscovered?
        assertThat(event)
            .isNotNull()
        assertThat(event!!.option.value.unpack(StringValue::class.java))
            .isEqualTo(StringValue.of("type.spine.io"))
    }

    @Test
    fun `produce type events`() {
        assertEmits(
            FileEntered::class,

            TypeEntered::class,
            TypeExited::class,

            FileExited::class
        )
    }

    @Test
    fun `produce field events`() {
        assertEmits(
            FileEntered::class,
            TypeEntered::class,

            FieldEntered::class,
            FieldOptionDiscovered::class,
            FieldExited::class,

            TypeExited::class,
            FileExited::class
        )
    }

    @Test
    fun `produce custom field option events`() {
        val event = events.find {
            it is FieldOptionDiscovered && it.option.number == REQUIRED_FIELD_NUMBER
        } as FieldOptionDiscovered?
        assertThat(event)
            .isNotNull()
        assertThat(event!!.option.value.unpack(BoolValue::class.java))
            .isEqualTo(BoolValue.of(true))
    }

    @Test
    fun `produce 'oneof' events`() {
        assertEmits(
            FileEntered::class,
            TypeEntered::class,

            OneofGroupEntered::class,
            FieldEntered::class,
            FieldOptionDiscovered::class,
            FieldExited::class,
            OneofGroupExited::class,

            TypeExited::class,
            FileExited::class
        )
    }
    
    @Test
    fun `produce enum events`() {
        assertEmits(
            FileEntered::class,
            EnumEntered::class,
            EnumConstantEntered::class,
            EnumConstantExited::class,
            EnumConstantEntered::class,
            EnumConstantExited::class,
            EnumConstantEntered::class,
            EnumConstantExited::class,
            EnumExited::class
        )
    }

    @Test
    fun `produce nested type events`() {
        assertEmits(
            TypeEntered::class,
            TypeEntered::class,
            TypeExited::class,
            TypeExited::class
        )
    }

    @Test
    fun `produce service events`() {
        assertEmits(
            ServiceEntered::class,
            RpcEntered::class,
            RpcOptionDiscovered::class,
            RpcExited::class,
            ServiceExited::class
        )
    }

    @Test
    fun `include 'rpc' options`() {
        val event = emitted<RpcOptionDiscovered>()
        assertThat(event.option.name)
            .isEqualTo("idempotency_level")
        assertThat(unpack(event.option.value))
            .isEqualTo(EnumValue
                .newBuilder()
                .setName(NO_SIDE_EFFECTS.name)
                .setNumber(NO_SIDE_EFFECTS_VALUE)
                .build())
    }

    @Test
    fun `include message doc info`() {
        val typeEntered = emitted<TypeEntered>()
        assertThat(typeEntered.type)
            .comparingExpectedFieldsOnly()
            .isEqualTo(MessageType
                .newBuilder()
                .setName(TypeName.newBuilder().setSimpleName("Journey"))
                .build())
        assertThat(typeEntered.type.doc.leadingComment)
            .isEqualTo("""
                A Doctor's journey.

                A test type

            """.trimIndent())
        assertThat(typeEntered.type.doc.trailingComment)
            .isEqualTo("Impl note: test type.")
        assertThat(typeEntered.type.doc.detachedCommentList)
            .containsExactly("Detached 1.", "Detached 2.")
    }

    private fun assertEmits(vararg types: KClass<out EventMessage>) {
        val javaClasses = types.map { it.java }
        assertThat(events)
            .comparingElementsUsing(type<EventMessage>())
            .containsAtLeastElementsIn(javaClasses)
            .inOrder()
    }

    private inline fun <reified E : EventMessage> emitted(): E {
        val javaClass = E::class.java
        return events.find { it.javaClass == javaClass }!! as E
    }
}
