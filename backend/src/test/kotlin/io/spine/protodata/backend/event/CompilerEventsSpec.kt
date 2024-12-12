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

package io.spine.protodata.backend.event

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS
import com.google.protobuf.DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS_VALUE
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import com.google.protobuf.compiler.codeGeneratorRequest
import com.google.protobuf.enumValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.base.EventMessage
import io.spine.option.OptionsProto
import io.spine.protobuf.unpackGuessingType
import io.spine.protodata.ast.event.EnumConstantEntered
import io.spine.protodata.ast.event.EnumConstantExited
import io.spine.protodata.ast.event.EnumEntered
import io.spine.protodata.ast.event.EnumExited
import io.spine.protodata.ast.event.FieldEntered
import io.spine.protodata.ast.event.FieldExited
import io.spine.protodata.ast.event.FieldOptionDiscovered
import io.spine.protodata.ast.event.FileEntered
import io.spine.protodata.ast.event.FileExited
import io.spine.protodata.ast.event.FileOptionDiscovered
import io.spine.protodata.ast.event.OneofGroupEntered
import io.spine.protodata.ast.event.OneofGroupExited
import io.spine.protodata.ast.event.RpcEntered
import io.spine.protodata.ast.event.RpcExited
import io.spine.protodata.ast.event.RpcOptionDiscovered
import io.spine.protodata.ast.event.ServiceEntered
import io.spine.protodata.ast.event.ServiceExited
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.ast.event.TypeEntered
import io.spine.protodata.ast.event.TypeExited
import io.spine.protodata.ast.file
import io.spine.protodata.ast.messageType
import io.spine.protodata.ast.typeName
import io.spine.protodata.test.DoctorProto
import io.spine.testing.Correspondences
import io.spine.type.KnownTypes
import kotlin.reflect.KClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the [CompilerEvents] class basing on the `doctor.proto`.
 *
 * See the file at `test-env/src/main/proto/spine/protodata/test/doctor.proto`.
 */
@DisplayName("`CompilerEvents` should")
class CompilerEventsSpec {

    private val nl: String = System.lineSeparator()

    private lateinit var events: List<EventMessage>

    @BeforeEach
    fun parseEvents() {
        // Gather files of all known types to simplify resolving dependencies.
        val allDependencyFiles = KnownTypes.instance()
            .asTypeSet()
            .messageTypes()
            .map { it.descriptor().file.toProto() }

        val request = codeGeneratorRequest {
            fileToGenerate += DoctorProto.getDescriptor().fullName
            protoFile.addAll(allDependencyFiles)
        }
        events = CompilerEvents.parse(request, { true }).toList()
    }

    @Test
    fun `not have duplicate events`() {
        events.distinct() shouldContainExactly events
    }

    @Nested
    inner class `produce file events` {

        @Test
        fun `events in order`() = assertEmits(
            FileEntered::class, // Starts with the file.
            FileOptionDiscovered::class, // Now come options.
            FileOptionDiscovered::class,
            FileOptionDiscovered::class,
            FileOptionDiscovered::class,
            FileExited::class // Ends with the file.
        )

        @Test
        fun `for standard file option`() = with(events) {
            findJavaPackageEvent().option<StringValue>().value shouldBe "io.spine.protodata.test"
            findOuterClassNameEvent().option<StringValue>().value shouldBe "DoctorProto"
            findMultipleFilesEvent().option<BoolValue>().value shouldBe true
        }

        @Test
        fun `for custom file option`() {
            val event = events.findTypeUrlPrefixEvent()

            event shouldNotBe null
            event.option<StringValue>().value shouldBe "type.spine.io"
        }
    }

    @Nested
    inner class `produce type events` {

        @Test
        fun `in order`() = assertEmits(
            FileEntered::class,

            TypeEntered::class,
            TypeExited::class,

            FileExited::class
        )

        @Test
        fun `matching nesting of types`() = assertEmits(
            TypeEntered::class,
            TypeEntered::class,
            TypeExited::class,
            TypeExited::class
        )
    }

    @Nested
    inner class `produce field events` {

        @Test
        fun `in order`() = assertEmits(
            FileEntered::class,
            TypeEntered::class,

            FieldEntered::class,
            FieldOptionDiscovered::class,
            FieldExited::class,

            TypeExited::class,
            FileExited::class
        )

        @Test
        fun `for custom field option`() {
            val event = events.findRequiredFieldOptionEvent()

            event shouldNotBe null
            event.option<BoolValue>().value shouldBe true
        }
    }

    @Test
    fun `produce 'oneof' events`() = assertEmits(
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
    fun `produce enum events`() = assertEmits(
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
    fun `produce service events`() = assertEmits(
        ServiceEntered::class,
        RpcEntered::class,
        RpcOptionDiscovered::class,
        RpcExited::class,
        ServiceExited::class
    )

    @Test
    fun `include 'rpc' options`() {
        val event = emitted<RpcOptionDiscovered>()

        event.option.name shouldBe "idempotency_level"
        event.option.value.unpackGuessingType() shouldBe enumValue {
            name = NO_SIDE_EFFECTS.name
            number = NO_SIDE_EFFECTS_VALUE
        }
    }

    @Test
    fun `include message doc info`() {
        val typeEntered = emitted<TypeDiscovered>()
        assertThat(typeEntered.type)
            .comparingExpectedFieldsOnly()
            .isEqualTo(messageType {
                name = typeName { simpleName = "Journey" }
                file = file { path = "spine/protodata/test/doctor.proto" }
            })

        val doc = typeEntered.type.doc
        doc.leadingComment.split(nl) shouldContainExactly listOf(
            "A Doctor's journey.",
            "",
            "A test type",
            ""
        )

        doc.trailingComment shouldBe "Impl note: test type."
        doc.detachedCommentList[0] shouldBe "Detached 1."

        doc.detachedCommentList[1].split(nl) shouldContainExactly listOf(
            "Detached 2.",
            "Indentation is not preserved in Protobuf.",
            "",
            "Bla bla!"
        )
    }

    @Test
    fun `parse repeated values of custom options`() = assertEmits(
        TypeEntered::class, // message Word
        FieldEntered::class, // string test
        FieldOptionDiscovered::class, // (rhyme)
        FieldOptionDiscovered::class, // (rhyme)
        FieldOptionDiscovered::class, // (rhyme)
        FieldExited::class,
        TypeExited::class,
    )

    private fun assertEmits(vararg types: KClass<out EventMessage>) {
        val javaClasses = types.map { it.java }
        assertThat(events)
            .comparingElementsUsing(Correspondences.type<EventMessage>())
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

private fun List<EventMessage>.findTypeUrlPrefixEvent(): FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == OptionsProto.TYPE_URL_PREFIX_FIELD_NUMBER
} as FileOptionDiscovered?

private fun List<EventMessage>.findJavaPackageEvent(): FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == FileOptions.JAVA_PACKAGE_FIELD_NUMBER
}  as FileOptionDiscovered?

private fun List<EventMessage>.findOuterClassNameEvent(): FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.option.number == FileOptions.JAVA_OUTER_CLASSNAME_FIELD_NUMBER
}  as FileOptionDiscovered?

private fun List<EventMessage>.findMultipleFilesEvent() : FileOptionDiscovered? = find {
    it is FileOptionDiscovered && it.isJavaMultipleFilesField()
} as FileOptionDiscovered?

private fun FileOptionDiscovered.isJavaMultipleFilesField() =
    option.number == FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER

private fun List<EventMessage>.findRequiredFieldOptionEvent(): FieldOptionDiscovered? = find {
    it is FieldOptionDiscovered && it.option.number == OptionsProto.REQUIRED_FIELD_NUMBER
} as FieldOptionDiscovered?
