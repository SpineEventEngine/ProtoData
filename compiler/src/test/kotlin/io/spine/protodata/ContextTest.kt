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
import com.google.protobuf.BoolValue
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.option.OptionsProto.BETA_TYPE_FIELD_NUMBER
import io.spine.protobuf.AnyPacker
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.test.DoctorProto
import io.spine.testing.server.blackbox.BlackBoxContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat as assertMessage

class `'ProtoData' context should` {

    @Test
    fun `contain 'ProtobufSource' file projection`() {
        val ctx = ProtoDataContext.builder().build()
        assertTrue(ctx.hasEntitiesOfType(ProtoSourceFileProjection::class.java))
    }

    @Test
    fun `construct 'ProtobufSource' based on a descriptor set`() {
        val ctx = BlackBoxContext.from(ProtoDataContext.builder())
        val protoDescriptor = DoctorProto.getDescriptor().toProto()
        val set = CodeGeneratorRequest
            .newBuilder()
            .addProtoFile(protoDescriptor)
            .addFileToGenerate(protoDescriptor.name)
            .build()
        ProtobufCompilerContext.emitted(CompilerEvents.parse(set))

        val path = DoctorProto.getDescriptor().path()
        val assertSourceFile = ctx.assertEntity(path, ProtoSourceFileProjection::class.java)
        assertSourceFile
            .exists()
        val actual = assertSourceFile.actual()!!.state() as ProtobufSourceFile

        val types = actual.typeMap
        val typeName = "type.spine.io/spine.protodata.test.Journey"
        assertThat(types)
            .containsKey(typeName)
        val journeyType = types[typeName]!!
        assertThat(journeyType.name.typeUrl())
            .isEqualTo(typeName)
        assertMessage(journeyType.optionList)
            .containsExactly(
                Option
                    .newBuilder()
                    .setName("beta_type")
                    .setNumber(BETA_TYPE_FIELD_NUMBER)
                    .setType(TYPE_BOOL.asType())
                    .setValue(AnyPacker.pack(BoolValue.of(true)))
                    .build()
            )
        assertThat(journeyType.fieldList)
            .hasSize(4)
        assertThat(journeyType.oneofGroupList)
            .hasSize(1)
        assertThat(journeyType.oneofGroupList[0].fieldList)
            .hasSize(2)
    }
}
