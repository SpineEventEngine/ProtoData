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
import io.spine.protodata.File
import io.spine.protodata.TypeName
import io.spine.protodata.backend.Documentation
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
import io.spine.protodata.event.typeEntered
import io.spine.protodata.event.typeExited
import io.spine.protodata.event.typeOptionDiscovered
import io.spine.protodata.messageType
import io.spine.protodata.name
import io.spine.protodata.oneofGroup

/**
 * Produces events for a message.
 */
internal class MessageCompilerEvents(
    private val file: File,
    private val documentation: Documentation
) {

    /**
     * Yields compiler events for the given message type.
     *
     * Opens with an [TypeEntered] event. Then go the events regarding the type metadata. Then go
     * the events regarding the fields. At last, closes with an [TypeExited] event.
     */
    internal suspend fun SequenceScope<EventMessage>.produceMessageEvents(
        desc: Descriptor,
        nestedIn: TypeName? = null
    ) {
        val typeName = desc.name()
        val path = file.path
        val messageType = messageType {
            name = typeName
            file = path
            if (nestedIn != null) {
                declaredIn = nestedIn
            }
            doc = documentation.forMessage(desc)
            nestedMessages.addAll(desc.nestedTypes.map { it.name() })
            nestedEnums.addAll(desc.enumTypes.map { it.name() })
        }
        yield(
            typeEntered {
                file = path
                type = messageType
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
            produceOneofEvents(typeName, it)
        }

        desc.fields
            .filter { it.realContainingOneof == null }
            .forEach { produceFieldEvents(typeName, it) }

        desc.nestedTypes.forEach {
            produceMessageEvents(nestedIn = typeName, desc = it)
        }

        val enums = EnumCompilerEvents(file, documentation)
        desc.enumTypes.forEach {
            enums.apply {
                produceEnumEvents(nestedIn = typeName, desc = it)
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
     * Opens with an [OneofGroupEntered] event. Then go the events regarding the group metadata.
     * Then go the events regarding the fields. At last, closes with an [OneofGroupExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceOneofEvents(
        typeName: TypeName,
        desc: OneofDescriptor
    ) {
        val oneofName = desc.name()
        val oneofGroup = oneofGroup {
            name = oneofName
            doc = documentation.forOneof(desc)
        }
        val path = file.path
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
            produceFieldEvents(typeName, it)
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
     * Opens with an [FieldEntered] event. Then go the events regarding the field options. At last,
     * closes with an [FieldExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceFieldEvents(
        typeName: TypeName,
        desc: FieldDescriptor
    ) {
        val fieldName = desc.name()
        val theField = buildField(desc, typeName, documentation)
        val path = file.path
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
