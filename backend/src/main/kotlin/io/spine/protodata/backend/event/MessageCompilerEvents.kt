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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import io.spine.base.EventMessage
import io.spine.protodata.ast.Documentation
import io.spine.protodata.ast.ProtoFileHeader
import io.spine.protodata.ast.event.FieldEntered
import io.spine.protodata.ast.event.FieldExited
import io.spine.protodata.ast.event.OneofGroupEntered
import io.spine.protodata.ast.event.OneofGroupExited
import io.spine.protodata.ast.event.fieldEntered
import io.spine.protodata.ast.event.fieldExited
import io.spine.protodata.ast.event.fieldOptionDiscovered
import io.spine.protodata.ast.event.oneofGroupEntered
import io.spine.protodata.ast.event.oneofGroupExited
import io.spine.protodata.ast.event.oneofOptionDiscovered
import io.spine.protodata.ast.event.typeDiscovered
import io.spine.protodata.ast.event.typeEntered
import io.spine.protodata.ast.event.typeExited
import io.spine.protodata.ast.event.typeOptionDiscovered
import io.spine.protodata.ast.oneofGroup
import io.spine.protodata.ast.produceOptionEvents
import io.spine.protodata.protobuf.buildField
import io.spine.protodata.protobuf.name
import io.spine.protodata.protobuf.nestedTypes
import io.spine.protodata.protobuf.toMessageType

/**
 * Produces events for a message.
 */
internal class MessageCompilerEvents(
    private val header: ProtoFileHeader
) {
    /**
     * Yields compiler events for the given message type.
     *
     * Starts with [TypeDiscovered][io.spine.protodata.ast.event.TypeDiscovered] and
     * [io.spine.protodata.ast.event.TypeEntered] events.
     * Then the events regarding the type metadata come.
     * Then go the events regarding the fields.
     * At last, closes with an [TypeExited][io.spine.protodata.ast.event.TypeExited] event.
     *
     * @param desc The descriptor of the message type.
     */
    internal suspend fun SequenceScope<EventMessage>.produceEvents(
        desc: Descriptor
    ) {
        val typeName = desc.name()
        val path = header.file
        val messageType = desc.toMessageType()
        yield(
            typeDiscovered {
                file = path
                type = messageType
            }
        )
        yield(
            typeEntered {
                file = path
                type = messageType.name
            }
        )
        produceOptionEvents(desc.options) {
            typeOptionDiscovered {
                file = path
                type = typeName
                option = it
            }
        }

        desc.realOneofs.forEach {
            produceOneofEvents(it)
        }

        desc.fields
            .filter { it.realContainingOneof == null }
            .forEach { produceFieldEvents(it) }

        desc.nestedTypes().forEach {
            produceEvents(desc = it)
        }

        val enums = EnumCompilerEvents(header)
        desc.enumTypes.forEach {
            enums.apply {
                produceEvents(desc = it)
            }
        }

        yield(
            typeExited {
                file = path
                type = typeName
            }
        )
    }

    /**
     * Yields compiler events for the given `oneof` group.
     *
     * Opens with an [OneofGroupEntered] event.
     * Then go the events regarding the group metadata.
     * Then go the events regarding the fields.
     * At last, closes with an [OneofGroupExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceOneofEvents(
        desc: OneofDescriptor
    ) {
        val typeName = desc.containingType.name()
        val documentation = Documentation.of(desc.containingType.file)
        val oneofName = desc.name()
        val oneofGroup = oneofGroup {
            name = oneofName
            doc = documentation.forOneof(desc)
        }
        val path = header.file
        yield(
            oneofGroupEntered {
                file = path
                type = typeName
                group = oneofGroup
            }
        )
        produceOptionEvents(desc.options) {
            oneofOptionDiscovered {
                file = path
                type = typeName
                group = oneofName
                option = it
            }
        }
        desc.fields.forEach {
            produceFieldEvents(it)
        }
        yield(
            oneofGroupExited {
                file = path
                type = typeName
                group = oneofName
            }
        )
    }

    /**
     * Yields compiler events for the given field.
     *
     * Opens with an [FieldEntered] event.
     * Then events regarding the field options are emitted.
     * At last, closes with an [FieldExited] event.
     */
    @Suppress("DEPRECATION") /* Populate deprecated fields in `FieldOptionDiscovered`
        for backward compatibility. */
    private suspend fun SequenceScope<EventMessage>.produceFieldEvents(
        desc: FieldDescriptor
    ) {
        val typeName = desc.containingType.name()
        val fieldName = desc.name()
        val theField = buildField(desc)
        val path = header.file
        yield(
            fieldEntered {
                file = path
                type = typeName
                field = theField
            }
        )
        produceOptionEvents(desc.options) {
            fieldOptionDiscovered {
                file = path
                type = typeName
                field = fieldName
                subject = theField
                option = it
            }
        }
        yield(
            fieldExited {
                file = path
                type = typeName
                field = fieldName
            }
        )
    }
}
