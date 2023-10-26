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

import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import io.spine.base.EventMessage
import io.spine.protodata.File
import io.spine.protodata.TypeName
import io.spine.protodata.backend.Documentation
import io.spine.protodata.constantName
import io.spine.protodata.enumType
import io.spine.protodata.event.EnumConstantEntered
import io.spine.protodata.event.EnumConstantExited
import io.spine.protodata.event.EnumEntered
import io.spine.protodata.event.EnumExited
import io.spine.protodata.event.enumConstantEntered
import io.spine.protodata.event.enumConstantExited
import io.spine.protodata.event.enumConstantOptionDiscovered
import io.spine.protodata.event.enumEntered
import io.spine.protodata.event.enumExited
import io.spine.protodata.event.enumOptionDiscovered
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
        desc: EnumDescriptor,
        nestedIn: TypeName? = null
    ) {
        val typeName = desc.name()
        val path = file.path
        val type = enumType {
            name = typeName
            file = path
            if (nestedIn != null) {
                declaredIn = nestedIn
            }
            doc = documentation.forEnum(desc)
        }
        yield(
            enumEntered {
                file = path
                this.type = type
            }
        )
        produceOptionEvents(desc.options) {
            enumOptionDiscovered {
                file = path
                this.type = typeName
                option = it
            }
        }
        desc.values.forEach {
            produceConstantEvents(typeName, it)
        }
        yield(
            enumExited {
                file = path
                this.type = typeName
            }
        )
    }

    /**
     * Yields compiler events for the given enum constant.
     *
     * Opens with an [EnumConstantEntered] event. Then go the events regarding the constant options.
     * At last, closes with an [EnumConstantExited] event.
     */
    private suspend fun SequenceScope<EventMessage>.produceConstantEvents(
        typeName: TypeName,
        desc: EnumValueDescriptor
    ) {
        val name = constantName {
            value = desc.name
        }
        val theConstant = buildConstant(desc, typeName, documentation)
        val path = file.path
        yield(
            enumConstantEntered {
                file = path
                type = typeName
                constant = theConstant
            }
        )
        produceOptionEvents(desc.options) {
            enumConstantOptionDiscovered {
                file = path
                type = typeName
                constant = name
            }
        }
        yield(
            enumConstantExited {
                file = path
                type = typeName
                constant = name
            }
        )
    }
}
