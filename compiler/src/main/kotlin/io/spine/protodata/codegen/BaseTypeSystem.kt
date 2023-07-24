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

package io.spine.protodata.codegen

import io.spine.protodata.PrimitiveType
import io.spine.protodata.Type
import io.spine.protodata.TypeName
import io.spine.protodata.Value
import io.spine.protodata.Value.KindCase.BOOL_VALUE
import io.spine.protodata.Value.KindCase.BYTES_VALUE
import io.spine.protodata.Value.KindCase.DOUBLE_VALUE
import io.spine.protodata.Value.KindCase.ENUM_VALUE
import io.spine.protodata.Value.KindCase.INT_VALUE
import io.spine.protodata.Value.KindCase.LIST_VALUE
import io.spine.protodata.Value.KindCase.MAP_VALUE
import io.spine.protodata.Value.KindCase.MESSAGE_VALUE
import io.spine.protodata.Value.KindCase.NULL_VALUE
import io.spine.protodata.Value.KindCase.STRING_VALUE

public abstract class BaseTypeSystem<T: CodePrintable, V: CodePrintable>
protected constructor(
    private val knownTypes: Map<TypeName, T>
) {

    protected abstract val valueConverter: ValueConverter<V>

    public fun typeNameToCode(type: Type): T {
        return when {
            type.hasPrimitive() -> convertPrimitiveType(type.primitive)
            type.hasMessage() -> convertTypeName(type.message)
            type.hasEnumeration() -> convertTypeName(type.enumeration)
            else -> unknownType(type)
        }
    }

    protected abstract fun convertPrimitiveType(type: PrimitiveType): T

    public fun convertTypeName(protoName: TypeName): T =
        knownTypes[protoName] ?: unknownType(protoName)

    public fun valueToCode(value: Value): V = with(valueConverter) {
        when (value.kindCase) {
            NULL_VALUE -> toNull(value)
            BOOL_VALUE -> toBool(value)
            DOUBLE_VALUE -> toDouble(value)
            INT_VALUE -> toInt(value)
            STRING_VALUE -> toString(value)
            BYTES_VALUE -> toBytes(value)
            MESSAGE_VALUE -> toMessage(value)
            ENUM_VALUE -> toEnum(value)
            LIST_VALUE -> toList(value)
            MAP_VALUE -> toMap(value)
            else -> throw IllegalArgumentException("Empty value")
        }
    }

    public interface ValueConverter<V: CodePrintable> {

        /**
         * Converts the given `null` value into a language-specific `null` representation.
         */
        public fun toNull(value: Value): V

        /**
         * Converts the given `bool` value into a language-specific `bool` representation.
         */
        public fun toBool(value: Value): V

        /**
         * Converts the given `double` value into a language-specific `double` representation.
         */
        public fun toDouble(value: Value): V

        /**
         * Converts the given `int` value into a language-specific `int` representation.
         */
        public fun toInt(value: Value): V

        /**
         * Converts the given `string` value into a language-specific `string` representation.
         */
        public fun toString(value: Value): V

        /**
         * Converts the given `bytes` value into a language-specific syte string representation.
         */
        public fun toBytes(value: Value): V

        /**
         * Converts the given message value into a language-specific message representation.
         */
        public fun toMessage(value: Value): V

        /**
         * Converts the given enum constant into a language-specific enum representation.
         */
        public fun toEnum(value: Value): V

        /**
         * Converts the given list into a language-specific representation of a list of values.
         */
        public fun toList(value: Value): V

        /**
         * Converts the given map into a language-specific map.
         */
        public fun toMap(value: Value): V
    }
}

private fun unknownType(type: Type): Nothing =
    error("Unknown type: `${type}`.")

private fun unknownType(typeName: TypeName): Nothing =
    error("Unknown type: `${typeName.typeUrl}`.")
