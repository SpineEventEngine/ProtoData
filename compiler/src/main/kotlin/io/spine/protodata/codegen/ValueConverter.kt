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

import io.spine.protodata.Type
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

/**
 * A factory of language-specific code, that represents a Protobuf value.
 */
@Suppress("TooManyFunctions")
public abstract class ValueConverter<V: CodeElement> {

    /**
     * Converts the given Protobuf value into a language-specific code expression.
     *
     * @see ValueConverter
     */
    public fun valueToCode(value: Value): V =
        when (value.kindCase) {
            NULL_VALUE -> toNull(value.type)
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

    /**
     * Converts the `null` value of the given type into a language-specific `null` representation.
     */
    protected abstract fun toNull(type: Type): V

    /**
     * Converts the given `bool` value into a language-specific `bool` representation.
     */
    protected abstract fun toBool(value: Value): V

    /**
     * Converts the given `double` value into a language-specific `double` representation.
     */
    protected abstract fun toDouble(value: Value): V

    /**
     * Converts the given `int` value into a language-specific `int` representation.
     */
    protected abstract fun toInt(value: Value): V

    /**
     * Converts the given `string` value into a language-specific `string` representation.
     */
    protected abstract fun toString(value: Value): V

    /**
     * Converts the given `bytes` value into a language-specific syte string representation.
     */
    protected abstract fun toBytes(value: Value): V

    /**
     * Converts the given message value into a language-specific message representation.
     */
    protected abstract fun toMessage(value: Value): V

    /**
     * Converts the given enum constant into a language-specific enum representation.
     */
    protected abstract fun toEnum(value: Value): V

    /**
     * Converts the given list into a language-specific representation of a list of values.
     */
    protected abstract fun toList(value: Value): V

    /**
     * Converts the given map into a language-specific map.
     */
    protected abstract fun toMap(value: Value): V
}
