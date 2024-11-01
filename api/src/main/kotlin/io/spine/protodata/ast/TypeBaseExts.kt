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

package io.spine.protodata.ast

import io.spine.protodata.type.TypeSystem
import io.spine.string.shortly

/**
 * Obtains a simple name of the type if represents a message or an enum.
 *
 * @throws IllegalStateException If this is a primitive type.
 */
public val TypeBase.simpleName: String
    get() = typeName.simpleName

/**
 * Converts a message or an enum type to its [TypeName].
 *
 * @throws IllegalStateException if this type is not a message or an enum type.
 */
public val TypeBase.typeName: TypeName
    get() {
        messageOrEnumName?.let {
            return it
        }
        val unable = "Unable to obtain `${TypeName::class.simpleName}`"
        if (isPrimitive) {
            error("$unable for the primitive type `${primitive.name}`.")
        } else {
            // This is a safety net in case `Type` is extended with more `oneof` cases.
            error("$unable for the type `${shortly()}`.")
        }
    }

/**
 * The type name of this type, given that the type is a complex type and
 * not a Protobuf primitive type.
 *
 * If the type is primitive, this value is `null`.
 */
public val TypeBase.messageOrEnumName: TypeName?
    get() = when {
        isMessage -> message
        isEnum -> enumeration
        else -> null
    }

/**
 * Converts this type to an instance of [MessageType] finding it using the given [typeSystem].
 *
 * @throws IllegalStateException if this type is not a message type, or
 *   if the type system does not have a corresponding `MessageType`.
 */
public fun TypeBase.toMessageType(typeSystem: TypeSystem): MessageType {
    check(isMessage)
    return message.toMessageType(typeSystem)
}
