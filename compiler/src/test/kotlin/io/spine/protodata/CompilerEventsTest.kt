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

import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.BoolValue
import com.google.protobuf.DescriptorProtos.FileOptions.JAVA_MULTIPLE_FILES_FIELD_NUMBER
import com.google.protobuf.StringValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.base.EventMessage
import io.spine.option.OptionsProto.REQUIRED_FIELD_NUMBER
import io.spine.option.OptionsProto.TYPE_URL_PREFIX_FIELD_NUMBER
import io.spine.protodata.test.DoctorProto
import io.spine.testing.Correspondences.type
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class `'CompilerEvents' should` {

    private lateinit var events: List<EventMessage>

    @BeforeEach
    fun parseEvents() {
        val request = CodeGeneratorRequest
            .newBuilder()
            .addFileToGenerate(DoctorProto.getDescriptor().fullName)
            .addProtoFile(DoctorProto.getDescriptor().toProto())
            .build()
        events = CompilerEvents.parse(request).toList()
    }

    @Test
    fun `produce file events`() {
        assertEmits(
            FileEntered::class.java,
            FileOptionDiscovered::class.java,
            FileOptionDiscovered::class.java,
            FileExited::class.java
        )
    }

    @Test
    fun `produce standard file option events`() {
        val event = events.find {
            it is FileOptionDiscovered
                    && it.option.number == JAVA_MULTIPLE_FILES_FIELD_NUMBER
        } as FileOptionDiscovered?
        assertThat(event)
            .isNotNull()
        assertThat(event!!.option.value.unpack(BoolValue::class.java))
            .isEqualTo(BoolValue.of(true))
    }

    @Test
    fun `produce custom file option events`() {
        val event = events.find {
            it is FileOptionDiscovered
                    && it.option.number == TYPE_URL_PREFIX_FIELD_NUMBER
        } as FileOptionDiscovered?
        assertThat(event)
            .isNotNull()
        assertThat(event!!.option.value.unpack(StringValue::class.java))
            .isEqualTo(StringValue.of("type.spine.io"))
    }

    @Test
    fun `produce type events`() {
        assertEmits(
            FileEntered::class.java,

            TypeEntered::class.java,
            TypeExited::class.java,

            FileExited::class.java
        )
    }

    @Test
    fun `produce field events`() {
        assertEmits(
            FileEntered::class.java,
            TypeEntered::class.java,

            FieldEntered::class.java,
            FieldOptionDiscovered::class.java,
            FieldExited::class.java,

            TypeExited::class.java,
            FileExited::class.java
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
            FileEntered::class.java,
            TypeEntered::class.java,

            OneofGroupEntered::class.java,
            FieldEntered::class.java,
            FieldOptionDiscovered::class.java,
            FieldExited::class.java,
            OneofGroupExited::class.java,

            TypeExited::class.java,
            FileExited::class.java
        )
    }

    private fun assertEmits(vararg types: Class<out EventMessage>) {
        assertThat(events)
            .comparingElementsUsing(type<EventMessage>())
            .containsAtLeastElementsIn(types)
            .inOrder()
    }
}
