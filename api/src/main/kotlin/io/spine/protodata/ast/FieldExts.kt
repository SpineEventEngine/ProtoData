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

@file:JvmName("Fields")

package io.spine.protodata.ast

import io.spine.protodata.ast.Field.CardinalityCase
import io.spine.string.camelCase

/**
 * Tells if this field is a Protobuf message.
 */
public val Field.isMessage: Boolean
    get() = type.isMessage

/**
 * Shows if this field is a `map`.
 *
 * If the field is a `map`, the `Field.type` contains the type of the value, and
 * the `Field.map.key_type` contains the type the map key.
 */
public val Field.isMap: Boolean
    get() = hasMap()

/**
 * Shows if this field is a list.
 *
 * In Protobuf `repeated` keyword denotes a sequence of values for a field.
 * However, a map is also treated as a repeated field for serialization reasons.
 * We use the term "list" for repeated fields which are not maps.
 */
public val Field.isList: Boolean
    get() = hasList()

/**
 * Shows if this field repeated.
 *
 * Can be declared in Protobuf either as a `map` or a `repeated` field.
 */
public val Field.isRepeated: Boolean
    get() = isMap || isList

/**
 * Shows if this field is a part of a `oneof` group.
 *
 * If the field is a part of a `oneof`, the `Field.oneof_name` contains the name of that `oneof`.
 */
public val Field.isPartOfOneof: Boolean
    get() = hasOneofName()

/**
 * The field name containing a qualified name of the declaring type.
 */
public val Field.qualifiedName: String
    get() = "${declaringType.qualifiedName}.${name.value}"


/**
 * Obtains a `CamelCase` version of this field name.
 */
public val FieldName.camelCase: String
    get() = value.camelCase()

/**
 * Converts this [FieldType.KindCase] to [CardinalityCase],
 */
public fun FieldType.KindCase.toCardinalityCase(): CardinalityCase = when (this) {
    FieldType.KindCase.MESSAGE -> CardinalityCase.SINGLE
    FieldType.KindCase.ENUMERATION -> CardinalityCase.SINGLE
    FieldType.KindCase.PRIMITIVE -> CardinalityCase.SINGLE
    FieldType.KindCase.LIST -> CardinalityCase.LIST
    FieldType.KindCase.MAP -> CardinalityCase.MAP
    else -> error("Unable to transform `$this` to `CardinalityCase`.")
}

public fun Cardinality.toCardinalityCase(): CardinalityCase = when (this) {
    Cardinality.SINGLE -> CardinalityCase.SINGLE
    Cardinality.LIST -> CardinalityCase.LIST
    Cardinality.MAP -> CardinalityCase.MAP
    else -> error("Unable to transform `$this` to `CardinalityCase`.")
}

/**
 * Obtains a cardinality of this field type.
 *
 * @see Cardinality
 */
public val FieldType.cardinality: Cardinality
    get() {
        return when (kindCase) {
            FieldType.KindCase.MESSAGE,
            FieldType.KindCase.ENUMERATION,
            FieldType.KindCase.PRIMITIVE -> Cardinality.SINGLE
            FieldType.KindCase.LIST -> Cardinality.LIST
            FieldType.KindCase.MAP -> Cardinality.MAP
            else -> error("Unable to convert `$this` to `Cardinality`.")
        }
    }
