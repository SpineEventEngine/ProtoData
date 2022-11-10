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

package io.spine.protodata

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS_VALUE
import com.google.protobuf.EnumValue
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.codeGeneratorRequest
import io.spine.base.EventMessage
import io.spine.option.OptionsProto.REQUIRED_FIELD_NUMBER
import io.spine.option.OptionsProto.TYPE_URL_PREFIX_FIELD_NUMBER
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protodata.events.CompilerEvents
import io.spine.protodata.test.DoctorProto
import io.spine.testing.Correspondences.type
import io.spine.type.KnownTypes
import kotlin.reflect.KClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test



@DisplayName("`CompilerEvents` should")
class CompilerEventsSpec {

    private val nl: String = System.lineSeparator()

    private lateinit var events: List<EventMessage>

    @BeforeEach
    fun parseEvents() {
        val allTheTypes = KnownTypes.instance()
            .asTypeSet()
            .messageTypes()
            .map { it.descriptor().file.toProto() }

        val request = codeGeneratorRequest {
            fileToGenerate += DoctorProto.getDescriptor().fullName
            protoFile.addAll(allTheTypes)
        }
//            CodeGeneratorRequest.newBuilder()
//            .addFileToGenerate(DoctorProto.getDescriptor().fullName)
//            .addAllProtoFile(allTheTypes)
//            .build()
        events = CompilerEvents.parse(request).toList()
    }

    @Nested
    @DisplayName("produce")
    inner class Produce {

        @Test
        fun `file events`() = assertEmits(
            FileEntered::class,
            FileOptionDiscovered::class,
            FileOptionDiscovered::class,
            FileExited::class
        )

        @Test
        fun `standard file option events`() {
            val event = events.findMultipleFilesOptionEvent()
            assertThat(event).isNotNull()
            assertThat(event.option<BoolValue>().value)
                .isTrue()
        }

        @Test
        fun `custom file option events`() {
            val event = events.findTypeUrlPrefixEvent()
            assertThat(event).isNotNull()
            assertThat(event.option<StringValue>().value)
                .isEqualTo("type.spine.io")
        }

        @Test
        fun `type events`() = assertEmits(
            FileEntered::class,

            TypeEntered::class,
            TypeExited::class,

            FileExited::class
        )

        @Test
        fun `field events`() = assertEmits(
            FileEntered::class,
            TypeEntered::class,

            FieldEntered::class,
            FieldOptionDiscovered::class,
            FieldExited::class,

            TypeExited::class,
            FileExited::class
        )

        @Test
        fun `custom field option events`() {
            val event = events.findRequiredFieldOptionEvent()
            assertThat(event).isNotNull()
            assertThat(event.option<BoolValue>().value).isTrue()
        }

        @Test
        fun `'oneof' events`() = assertEmits(
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

        @Test
        fun `enum events`() = assertEmits(
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

        @Test
        fun `nested type events`() = assertEmits(
            TypeEntered::class,
            TypeEntered::class,
            TypeExited::class,
            TypeExited::class
        )

        @Test
        fun `service events`() = assertEmits(
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
            .isEqualTo(EnumValue.newBuilder()
                .setName(NO_SIDE_EFFECTS.name)
                .setNumber(NO_SIDE_EFFECTS_VALUE)
                .build())
    }

    @Test
    fun `include message doc info`() {
        val typeEntered = emitted<TypeEntered>()
        assertThat(typeEntered.type)
            .comparingExpectedFieldsOnly()
            .isEqualTo(MessageType.newBuilder()
                .setName(TypeName.newBuilder().setSimpleName("Journey"))
                .build())
        assertThat(typeEntered.type.doc.leadingComment.split(nl))
            .containsExactly("A Doctor's journey.", "", "A test type", "")
        assertThat(typeEntered.type.doc.trailingComment)
            .isEqualTo("Impl note: test type.")
        assertThat(typeEntered.type.doc.detachedCommentList[0])
            .isEqualTo("Detached 1.")
        assertThat(typeEntered.type.doc.detachedCommentList[1].split(nl))
            .containsExactly(
                "Detached 2.",
                "Indentation is not preserved in Protobuf.",
                "",
                "Bla bla!"
            )
    }

    @Test
    fun `parse repeated values of custom options`() = assertEmits(
        TypeEntered::class,
        FieldEntered::class,
        FieldOptionDiscovered::class,
        FieldOptionDiscovered::class,
        FieldOptionDiscovered::class,
        FieldExited::class,
        TypeExited::class,
    )

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

/**
 * Obtains the option of the given type [T] from this [FileOptionDiscovered] event.
 *
 * The receiver type is nullable for brevity of the calls after `isNotNull()`.
 */
private inline fun <reified T : Message> FileOptionDiscovered?.option() : T {
    return this!!.option.value.unpack(T::class.java)
}

/**
 * Obtains the option of the given type [T] from this [FieldOptionDiscovered] event.
 *
 * The receiver type is nullable for brevity of the calls after `isNotNull()`.
 */
private inline fun <reified T : Message> FieldOptionDiscovered?.option() : T {
    return this!!.option.value.unpack(T::class.java)
}

private fun List<EventMessage>.findMultipleFilesOptionEvent() : FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == JAVA_MULTIPLE_FILES_FIELD_NUMBER
} as FileOptionDiscovered?

private fun List<EventMessage>.findTypeUrlPrefixEvent(): FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == TYPE_URL_PREFIX_FIELD_NUMBER
} as FileOptionDiscovered?

private fun List<EventMessage>.findRequiredFieldOptionEvent(): FieldOptionDiscovered? = find {
    it is FieldOptionDiscovered && it.option.number == REQUIRED_FIELD_NUMBER
} as FieldOptionDiscovered?

