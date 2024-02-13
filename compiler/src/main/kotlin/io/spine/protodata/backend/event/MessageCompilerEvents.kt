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

package io.spine.protodata.backend.event

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import io.spine.base.EventMessage
import io.spine.protodata.Documentation
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.buildField
import io.spine.protodata.event.FieldEntered
import io.spine.protodata.event.FieldExited
import io.spine.protodata.event.OneofGroupEntered
import io.spine.protodata.event.OneofGroupExited
import io.spine.protodata.event.TypeEntered
import io.spine.protodata.event.TypeExited
import io.spine.protodata.event.fieldEntered
import io.spine.protodata.event.fieldExited
import io.spine.protodata.event.fieldOptionDiscovered
import io.spine.protodata.event.oneofGroupEntered
import io.spine.protodata.event.oneofGroupExited
import io.spine.protodata.event.oneofOptionDiscovered
import io.spine.protodata.event.typeDiscovered
import io.spine.protodata.event.typeEntered
import io.spine.protodata.event.typeExited
import io.spine.protodata.event.typeOptionDiscovered
import io.spine.protodata.name
import io.spine.protodata.oneofGroup
import io.spine.protodata.produceOptionEvents
import io.spine.protodata.toMessageType

/**
 * Produces events for a message.
 */
internal class MessageCompilerEvents(
    private val header: ProtoFileHeader,
    private val documentation: Documentation
) {
    /**
     * Yields compiler events for the given message type.
     *
     * Starts with an [TypeEntered] event.
     * Then the events regarding the type metadata come.
     * Then go the events regarding the fields.
     * At last, closes with an [TypeExited] event.
     *
     * @param desc
     *         the descriptor of a Protobuf [Message] type.
     */
    internal suspend fun SequenceScope<EventMessage>.produceMessageEvents(
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

        desc.nestedTypes.forEach {
            produceMessageEvents(desc = it)
        }

        val enums = EnumCompilerEvents(header)
        desc.enumTypes.forEach {
            enums.apply {
                produceEnumEvents(desc = it)
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
     * Then go the events regarding the field options.
     * At last, closes with an [FieldExited] event.
     */
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
