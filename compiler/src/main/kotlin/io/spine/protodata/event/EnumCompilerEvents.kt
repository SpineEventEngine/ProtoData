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

package io.spine.protodata.event

import com.google.protobuf.Descriptors
import io.spine.base.EventMessage
import io.spine.protodata.ConstantName
import io.spine.protodata.Documentation
import io.spine.protodata.EnumConstant
import io.spine.protodata.EnumType
import io.spine.protodata.File
import io.spine.protodata.TypeName
import io.spine.protodata.name

/**
 * Produces events for an enum.
 */
internal class EnumCompilerEvents(
    private val file: File,
    private val documentation: Documentation
) {

    /**
     * Yields compiler events for the given enum type.
     *
     * Opens with an [EnumEntered] event. Then go the events regarding the type metadata. Then go
     * the events regarding the enum constants. At last, closes with an [EnumExited] event.
     */
    internal suspend fun SequenceScope<EventMessage>.produceEnumEvents(
        descriptor: Descriptors.EnumDescriptor,
        nestedIn: TypeName? = null
    ) {
        val typeName = descriptor.name()
        val path = file.path
        val type = EnumType.newBuilder().apply {
                name = typeName
                file = path
                if (nestedIn != null) {
                    declaredIn = nestedIn
                }
                doc = documentation.forEnum(descriptor)
            }.build()
        yield(
            EnumEntered.newBuilder()
                .setFile(path)
                .setType(type)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            EnumOptionDiscovered.newBuilder()
                .setFile(path)
                .setType(typeName)
                .setOption(it)
                .build()
        }
        descriptor.values.forEach {
            produceConstantEvents(typeName, it)
        }
        yield(
            EnumExited.newBuilder()
                .setFile(path)
                .setType(typeName)
                .build()
        )
    }

    /**
     * Yields compiler events for the given enum constant.
     *
     * Opens with an [EnumConstantEntered] event. Then go the events regarding the constant options.
     * At last, closes with an [EnumConstantExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceConstantEvents(
        type: TypeName,
        descriptor: Descriptors.EnumValueDescriptor
    ) {
        val name = ConstantName.newBuilder()
            .setValue(descriptor.name)
            .build()
        val constant = EnumConstant.newBuilder()
            .setName(name)
            .setDeclaredIn(type)
            .setNumber(descriptor.number)
            .setOrderOfDeclaration(descriptor.index)
            .setDoc(documentation.forEnumConstant(descriptor))
            .build()
        val path = file.path
        yield(
            EnumConstantEntered.newBuilder()
                .setFile(path)
                .setType(type)
                .setConstant(constant)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            EnumConstantOptionDiscovered.newBuilder()
                .setFile(path)
                .setType(type)
                .setConstant(name)
                .build()
        }
        yield(
            EnumConstantExited.newBuilder()
                .setFile(path)
                .setType(type)
                .setConstant(name)
                .build()
        )
    }
}
