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

import io.spine.protodata.ast.FieldType.KindCase.ENUMERATION
import io.spine.protodata.ast.FieldType.KindCase.LIST
import io.spine.protodata.ast.FieldType.KindCase.MAP
import io.spine.protodata.ast.FieldType.KindCase.MESSAGE
import io.spine.protodata.ast.FieldType.KindCase.PRIMITIVE

/**
 * Obtains a human-readable name of this field type.
 */
public val FieldType.name: String
    get() = when (kindCase) {
        MESSAGE -> message.qualifiedName
        ENUMERATION -> enumeration.qualifiedName
        PRIMITIVE -> primitive.protoName
        LIST -> "repeated ${list.name}"
        MAP -> "map<${map.keyType.name}, ${map.valueType.name}"
        else -> kindCase.name
    }

/**
 * Converts this field type to [Type].
 *
 * @throws IllegalStateException If this is field type is a list or a map.
 */
public fun FieldType.toType(): Type = type {
    val self = this@toType
    val dsl = this@type
    when {
        isMessage -> dsl.message = self.message
        isEnum -> dsl.enumeration = self.enumeration
        isPrimitive -> dsl.primitive = self.primitive
        else -> error("Cannot convert $self to `${Type::class.simpleName}`.")
    }
}

/**
 * Tells if this field type represents a message.
 */
public val FieldType.isMessage: Boolean
    get() = hasMessage()

/**
 * Tells if this field type represents an enum.
 */
public val FieldType.isEnum: Boolean
    get() = hasEnumeration()

/**
 * Tells if this field type represents a primitive value.
 */
public val FieldType.isPrimitive: Boolean
    get() = hasPrimitive()

/**
 * Tells if this field is `repeated`, but not a `map`.
 */
public val FieldType.isList: Boolean
    get() = hasList()

/**
 * Tells if this field type represents a message.
 */
public val FieldType.isMap: Boolean
    get() = hasMap()

/**
 * Obtains a cardinality of this field type.
 *
 * @see Cardinality
 */
public val FieldType.cardinality: Cardinality
    get() = when (kindCase) {
        MESSAGE,
        ENUMERATION,
        PRIMITIVE -> Cardinality.SINGLE
        LIST -> Cardinality.LIST
        MAP -> Cardinality.MAP
        else -> error("Unable to convert `$kindCase` to `Cardinality`.")
    }
