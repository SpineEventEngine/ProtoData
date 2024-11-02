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

import com.google.protobuf.Message
import io.spine.annotation.GeneratedMixin

/**
 * The common interface for [Type] and [FieldType].
 */
@GeneratedMixin
public interface TypeBase: Message {

    /**
     * A mix-in method for being implemented by the generated class, indicating whether
     * this type is a message.
     *
     * @see isMessage
     */
    public fun hasMessage(): Boolean

    /**
     * A mix-in method for being implemented by the generated class,indicating whether
     * this type is an enum.
     *
     * @see isEnum
     */
    public fun hasEnumeration(): Boolean

    /**
     * A mix-in method for being implemented by the generated class, indicating whether
     * this is a primitive type.
     *
     * @see isPrimitive
     */
    public fun hasPrimitive(): Boolean

    /**
     * Tells if this type represents a message.
     */
    public val isMessage: Boolean
        get() = hasMessage()

    /**
     * Tells if this type represents an enum.
     */
    public val isEnum: Boolean
        get() = hasEnumeration()

    /**
     * Tells if this type represents a primitive value.
     */
    public val isPrimitive: Boolean
        get() = hasPrimitive()

    /**
     * Tells if this type is `google.protobuf.Any`.
     */
    public val isAny: Boolean
        get() = isMessage && message.isAny

    /**
     * Obtains the message type name if this type is a message, or
     * [default instance][TypeName.getDefaultInstance] otherwise.
     */
    public val message: TypeName

    /**
     * Obtains the enum type name if this type is a message, or
     * [default instance][TypeName.getDefaultInstance] otherwise.
     */
    public val enumeration: TypeName

    /**
     * Obtains this type as a [PrimitiveType] enum item if this type is primitive,
     * returning [PrimitiveType.PT_UNKNOWN] otherwise.
     */
    public val primitive: PrimitiveType
}
