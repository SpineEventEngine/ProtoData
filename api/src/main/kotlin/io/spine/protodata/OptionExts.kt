/*
 * Copyright 2024, TeamDev. All rights reserved.
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

@file:JvmName("Options")

package io.spine.protodata

import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type.ENUM
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage
import com.google.protobuf.Message
import io.spine.base.EventMessage
import io.spine.option.OptionsProto
import io.spine.protobuf.TypeConverter
import io.spine.protobuf.pack

/**
 * Tells if this is a column option.
 */
public val Option.isColumn: Boolean
    get() = name == OptionsProto.column.descriptor.name

/**
 * Looks up an option value by the [optionName].
 *
 * If the option has a Protobuf primitive type, [cls] must be the wrapper type.
 * For example, an `Int32Value` for `int32`, `StringValue` for `string`, etc.
 *
 * @return the value of the option or `null` if the option is not found.
 */
public fun <T : Message> Iterable<Option>.find(optionName: String, cls: Class<T>): T? {
    val value = firstOrNull { it.name == optionName }?.value
    return value?.unpack(cls)
}

/**
 * Yields events regarding a set of options.
 *
 * @param options
 *         the set of options, such as `FileOptions`, `FieldOptions`, etc.
 * @param factory
 *         a function which given an option, constructs a fitting event.
 */
public suspend fun SequenceScope<EventMessage>.produceOptionEvents(
    options: ExtendableMessage<*>,
    factory: (Option) -> EventMessage
) {
    parseOptions(options).forEach {
        yield(factory(it))
    }
}

/**
 * Parses this `options` message into a list of [Option]s.
 */
public fun ExtendableMessage<*>.toList(): List<Option> =
    parseOptions(this).toList()

private fun parseOptions(options: ExtendableMessage<*>): Sequence<Option> =
    sequence {
        options.allFields.forEach { (optionDescriptor, value) ->
            if (value is Collection<*>) {
                value.forEach {
                    val option = toOption(optionDescriptor, it!!)
                    yield(option)
                }
            } else {
                val option = toOption(optionDescriptor, value)
                yield(option)
            }
        }
    }

private fun toOption(
    optionDescriptor: FieldDescriptor,
    value: Any
): Option {
    val optionValue = fieldToAny(optionDescriptor, value)
    val option = option {
        name = optionDescriptor.name
        number = optionDescriptor.number
        type = optionDescriptor.type()
        this.value = optionValue
    }
    return option
}

private fun fieldToAny(field: FieldDescriptor, value: Any): com.google.protobuf.Any =
    if (field.type == ENUM) {
        val descr = value as EnumValueDescriptor
        val enumValue = com.google.protobuf.enumValue {
            name = descr.name
            number = descr.number
        }
        enumValue.pack()
    } else {
        TypeConverter.toAny(value)
    }

