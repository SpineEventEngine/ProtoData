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

package io.spine.protodata.type

import com.google.protobuf.ByteString
import io.spine.protodata.ast.Type
import io.spine.protodata.value.EnumValue
import io.spine.protodata.value.ListValue
import io.spine.protodata.value.MapValue
import io.spine.protodata.value.MessageValue
import io.spine.protodata.value.Reference
import io.spine.protodata.value.Value
import io.spine.protodata.value.Value.KindCase.BOOL_VALUE
import io.spine.protodata.value.Value.KindCase.BYTES_VALUE
import io.spine.protodata.value.Value.KindCase.DOUBLE_VALUE
import io.spine.protodata.value.Value.KindCase.ENUM_VALUE
import io.spine.protodata.value.Value.KindCase.INT_VALUE
import io.spine.protodata.value.Value.KindCase.LIST_VALUE
import io.spine.protodata.value.Value.KindCase.MAP_VALUE
import io.spine.protodata.value.Value.KindCase.MESSAGE_VALUE
import io.spine.protodata.value.Value.KindCase.NULL_VALUE
import io.spine.protodata.value.Value.KindCase.STRING_VALUE
import io.spine.protodata.value.Value.KindCase.REFERENCE
import io.spine.tools.code.Language

/**
 * A factory of language-specific code that represents a Protobuf value.
 *
 * @param L the programming language served by this converter.
 * @param C the type of the code elements produced by this converter.
 */
@Suppress("TooManyFunctions")
public abstract class ValueConverter<L: Language, C: CodeElement<L>> {

    /**
     * Converts the given Protobuf value into a language-specific code expression.
     */
    public fun valueToCode(value: Value): C =
        when (value.kindCase) {
            NULL_VALUE -> nullToCode(value.type)
            BOOL_VALUE -> toCode(value.boolValue)
            DOUBLE_VALUE -> toCode(value.doubleValue)
            INT_VALUE -> toCode(value.intValue)
            STRING_VALUE -> this.toCode(value.stringValue)
            BYTES_VALUE -> this.toCode(value.bytesValue)
            MESSAGE_VALUE -> this.toCode(value.messageValue)
            ENUM_VALUE -> this.toCode(value.enumValue)
            LIST_VALUE -> toList(value.listValue)
            MAP_VALUE -> this.toCode(value.mapValue)
            REFERENCE -> this.toCode(value.reference)
            else -> throw IllegalArgumentException("Unsupported value kind: `${value.kindCase}`.")
        }

    /**
     * Converts the `null` value of the given type into a language-specific `null` representation.
     */
    protected abstract fun nullToCode(type: Type): C

    /**
     * Converts the given `bool` value into a language-specific `bool` representation.
     */
    protected abstract fun toCode(value: Boolean): C

    /**
     * Converts the given `double` value into a language-specific `double` representation.
     */
    protected abstract fun toCode(value: Double): C

    /**
     * Converts the given `int` value into a language-specific representation.
     */
    protected abstract fun toCode(value: Long): C

    /**
     * Converts the given `string` value into a language-specific `string` representation.
     */
    protected abstract fun toCode(value: String): C

    /**
     * Converts the given `bytes` value into a language-specific byte string representation.
     */
    protected abstract fun toCode(value: ByteString): C

    /**
     * Converts the given message value into a language-specific message representation.
     */
    protected abstract fun toCode(value: MessageValue): C

    /**
     * Converts the given enum constant into a language-specific enum representation.
     */
    protected abstract fun toCode(value: EnumValue): C

    /**
     * Converts the given list into a language-specific representation of a list of values.
     */
    protected abstract fun toList(value: ListValue): C

    /**
     * Converts the given map into a language-specific map.
     */
    protected abstract fun toCode(value: MapValue): C

    /**
     * Converts the given field reference to a language-specific accessor of the referenced field.
     */
    protected abstract fun toCode(reference: Reference): C
}
