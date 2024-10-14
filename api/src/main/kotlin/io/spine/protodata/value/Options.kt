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
import io.spine.base.fieldPath
import io.spine.option.MaxOption
import io.spine.option.MinOption
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.field
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.type.TypeSystem

/**
 * Parses the value of this `(min)` option into an instance of [Value].
 *
 * The value could be an integer, double, or a reference to another field.
 */
public fun MinOption.parse(field: Field, typeSystem: TypeSystem): Value =
    OptionValue("min", value, field, typeSystem).parse()

/**
 * Parses the value of this `(min)` option into an instance of [Value].
 *
 * The value could be an integer, double, or a reference to another field.
 */
public fun MaxOption.parse(field: Field, typeSystem: TypeSystem): Value =
    OptionValue("max", value, field, typeSystem).parse()

/**
 * Parses the value of the option into an instance of [Value].
 *
 * Supported value formats are [INTEGER], [DOUBLE], and [REFERENCE].
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
            value.matches(REFERENCE) -> value {
                reference = reference {
                    type = field.type
                    target = ensureField(value.toFieldPath())
                }
            }
            else -> error("${optionPath()} has the value with unexpected format (`$value`).")
        }
    }

    private fun optionPath() =
        "The `($optionName)` option declared in the field `${field.name.value}` of" +
                " the type `${field.type.message.qualifiedName}`"

    private fun ensureField(path: FieldPath): FieldPath {
        var typeName = field.declaringType
        var remaining = path
        while (remaining.fieldNameList.isNotEmpty()) {
            val message = typeSystem.findMessage(typeName)!!.first
            val current = remaining.fieldNameList.first()
            check(message.fieldList.any { it.name.value == current }) {
                "Unable to find the field named `$current`" +
                        " in the message type `${message.qualifiedName}`."
            }
            remaining = remaining.stepInto()
            if (remaining.isEmpty()) {
                continue
            }
            val nextField = message.field(current)
            check(nextField.isMessage) {
                "The field `$current` declared in the message type `${message.qualifiedName}`" +
                    " is not of a message type and cannot be used for referencing a nested field."
            }
            typeName = nextField.type.message
        }
        return path
    }

    private companion object {
        private val INTEGER = Regex("^-?\\d+$")
        private val DOUBLE = Regex("^-?\\d+\\.\\d+$")

        /**
         * The regex for field references which could be fields in the same type or
         * nested fields.
         *
         * We deliberately relax the expression to allow references starting from capital
         * letters as well. This is to allow users to use custom field conventions should
         * they have such a need.
         */
        private val REFERENCE = Regex("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$")
    }
}

private fun String.toFieldPath(): FieldPath = fieldPath {
    split(".").forEach { fieldName.add(it) }
}

private fun FieldPath.stepInto(): FieldPath = fieldPath {
    val withoutFirst = fieldNameList.toMutableList().also { it.removeAt(0) }
    fieldName.addAll(withoutFirst)
}

private fun FieldPath.isEmpty(): Boolean = fieldNameList.isEmpty()
