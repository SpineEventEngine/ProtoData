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

@file:JvmName("Options")

package io.spine.protodata.value

import io.spine.base.FieldPath
import io.spine.base.FieldPathConstants
import io.spine.base.joined
import io.spine.option.MaxOption
import io.spine.option.MinOption
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.type.TypeSystem
import io.spine.protodata.type.resolve
import io.spine.protodata.value.OptionValue.Companion.DOUBLE
import io.spine.protodata.value.OptionValue.Companion.INTEGER

/**
 * Parses the value of this `(min)` option into an instance of [Value].
 *
 * The value could be an integer, double, or a reference to another field.
 */
public fun MinOption.parse(field: Field, typeSystem: TypeSystem): Value =
    OptionValue("min", value, field, typeSystem).parse()

/**
 * Parses the value of this `(max)` option into an instance of [Value].
 *
 * The value could be an integer, double, or a reference to another field.
 */
public fun MaxOption.parse(field: Field, typeSystem: TypeSystem): Value =
    OptionValue("max", value, field, typeSystem).parse()

/**
 * Parses the value of the option into an instance of [Value].
 *
 * Supported value formats are [INTEGER], [DOUBLE], and [FieldPathConstants.REGEX].
 * Values in other formats will cause [IllegalStateException].
 */
private class OptionValue(
    private val optionName: String,
    private val value: String,
    private val field: Field,
    private val typeSystem: TypeSystem
) {
    init {
        check(value.isNotEmpty()) {
            "${optionPath()} cannot be empty."
        }
    }

    fun parse(): Value {
        return when {
            value.matches(INTEGER) -> value { intValue = value.toLong() }
            value.matches(DOUBLE) -> value { doubleValue = value.toDouble() }
            value.matches(FieldPathConstants.REGEX) -> value {
                reference = reference {
                    type = field.type
                    target = checkFieldReference(FieldPath(value))
                }
            }
            else -> error("${optionPath()} has the value with unexpected format (`$value`).")
        }
    }

    private val sourceFieldName: String by lazy {
        field.name.value
    }

    private val messageTypeName: String by lazy {
        field.type.message.qualifiedName
    }

    private fun optionPath() =
        "The `($optionName)` option declared for the field `$sourceFieldName` of" +
                " the type `$messageTypeName`"

    /**
     * Ensures that the field specified in the [path] exists and is of
     * the same type as [field].
     *
     * @throws IllegalStateException if one of the conditions above is not met.
     */
    private fun checkFieldReference(path: FieldPath): FieldPath {
        val typeName = field.declaringType
        val message = typeSystem.findMessage(typeName)!!.first
        val referencedField = typeSystem.resolve(path, message)
        check(referencedField.type == field.type) {
            val referencedFieldPath = path.joined
            "The field `$referencedFieldPath` referenced in the `($optionName).value` option" +
                    " of the field `$sourceFieldName` of the type `$messageTypeName`" +
                    " is of type `${referencedField.type.name}`" +
                    " but the field `$sourceFieldName` is of type `${field.type.name}`." +
                    " Please correct the field reference or change the type of `$sourceFieldName`."
        }
        return path
    }

    private companion object {
        private val INTEGER = Regex("^-?\\d+$")
        private val DOUBLE = Regex("^-?\\d+\\.\\d+$")
    }
}
