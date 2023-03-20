/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import com.google.protobuf.Descriptors
import com.google.protobuf.Empty
import io.spine.protodata.Field
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.OneofGroup
import io.spine.protodata.TypeName
import io.spine.protodata.event.CompilerEvent
import io.spine.protodata.event.FieldEntered
import io.spine.protodata.event.FieldExited
import io.spine.protodata.event.FieldOptionDiscovered
import io.spine.protodata.event.OneofGroupEntered
import io.spine.protodata.event.OneofGroupExited
import io.spine.protodata.event.OneofOptionDiscovered
import io.spine.protodata.event.TypeEntered
import io.spine.protodata.event.TypeExited
import io.spine.protodata.event.TypeOptionDiscovered
import io.spine.protodata.name

/**
 * Produces events for a message.
 */
internal class MessageCompilerEvents(
    private val file: File,
    private val documentation: Documentation,
    private val shouldGenerate: Boolean
) {

    /**
     * Yields compiler events for the given message type.
     *
     * Opens with an [TypeEntered] event. Then go the events regarding the type metadata. Then go
     * the events regarding the fields. At last, closes with an [TypeExited] event.
     */
    internal suspend fun SequenceScope<CompilerEvent>.produceMessageEvents(
        descriptor: Descriptors.Descriptor,
        nestedIn: TypeName? = null
    ) {
        val typeName = descriptor.name()
        val path = file.path
        val type = MessageType.newBuilder().apply {
                name = typeName
                file = path
                if (nestedIn != null) {
                    declaredIn = nestedIn
                }
                doc = documentation.forMessage(descriptor)
            }.build()
        yield(
            TypeEntered.newBuilder()
                .setFile(path)
                .setType(type)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            TypeOptionDiscovered.newBuilder()
                .setFile(path)
                .setType(typeName)
                .setOption(it)
                .setGenerationRequested(shouldGenerate)
                .build()
        }

        descriptor.realOneofs.forEach { produceOneofEvents(typeName, it) }

        descriptor.fields
            .filter { it.realContainingOneof == null }
            .forEach { produceFieldEvents(typeName, it) }

        descriptor.nestedTypes.forEach {
            produceMessageEvents(nestedIn = typeName, descriptor = it)
        }

        val enums = EnumCompilerEvents(file, documentation, shouldGenerate)
        descriptor.enumTypes.forEach {
            enums.apply {
                produceEnumEvents(nestedIn = typeName, descriptor = it)
            }
        }

        yield(
            TypeExited.newBuilder()
                .setFile(path)
                .setType(typeName)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
    }

    /**
     * Yields compiler events for the given `oneof` group.
     *
     * Opens with an [OneofGroupEntered] event. Then go the events regarding the group metadata.
     * Then go the events regarding the fields. At last, closes with an [OneofGroupExited] event.
     */
    private suspend fun SequenceScope<CompilerEvent>.produceOneofEvents(
        type: TypeName,
        descriptor: Descriptors.OneofDescriptor
    ) {
        val oneofName = descriptor.name()
        val oneofGroup = OneofGroup.newBuilder()
            .setName(oneofName)
            .setDoc(documentation.forOneof(descriptor))
            .build()
        val path = file.path
        yield(
            OneofGroupEntered.newBuilder()
                .setFile(path)
                .setType(type)
                .setGroup(oneofGroup)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            OneofOptionDiscovered.newBuilder()
                .setFile(path)
                .setType(type)
                .setGroup(oneofName)
                .setOption(it)
                .setGenerationRequested(shouldGenerate)
                .build()
        }
        descriptor.fields.forEach { produceFieldEvents(type, it) }
        yield(
            OneofGroupExited.newBuilder()
                .setFile(path)
                .setType(type)
                .setGroup(oneofName)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
    }

    /**
     * Yields compiler events for the given field.
     *
     * Opens with an [FieldEntered] event. Then go the events regarding the field options. At last,
     * closes with an [FieldExited] event.
     */
    private suspend fun SequenceScope<CompilerEvent>.produceFieldEvents(
        type: TypeName,
        descriptor: Descriptors.FieldDescriptor
    ) {
        val fieldName = descriptor.name()
        val field = Field.newBuilder()
            .setName(fieldName)
            .setDeclaringType(type)
            .setNumber(descriptor.number)
            .setOrderOfDeclaration(descriptor.index)
            .assignTypeAndCardinality(descriptor)
            .setDoc(documentation.forField(descriptor))
            .build()
        val path = file.path
        yield(
            FieldEntered.newBuilder()
                .setFile(path)
                .setType(type)
                .setField(field)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            FieldOptionDiscovered.newBuilder()
                .setFile(path)
                .setType(type)
                .setField(fieldName)
                .setOption(it)
                .setGenerationRequested(shouldGenerate)
                .build()
        }
        yield(
            FieldExited.newBuilder()
                .setFile(path)
                .setType(type)
                .setField(fieldName)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
    }

    /**
     * Assigns the field type and cardinality (`map`/`list`/`oneof_name`/`single`) to the receiver
     * builder.
     *
     * @return the receiver for method chaining.
     */
    private fun Field.Builder.assignTypeAndCardinality(
        desc: Descriptors.FieldDescriptor
    ): Field.Builder {
        if (desc.isMapField) {
            val (keyField, valueField) = desc.messageType.fields
            map = Field.OfMap.newBuilder()
                .setKeyType(keyField.primitiveType())
                .build()
            type = valueField.type()
        } else {
            type = desc.type()
            when {
                desc.isRepeated -> list = Empty.getDefaultInstance()
                desc.realContainingOneof != null -> oneofName = desc.realContainingOneof.name()
                else -> single = Empty.getDefaultInstance()
            }
        }
        return this
    }
}
