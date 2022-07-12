/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.protodata.events

import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type.ENUM
import com.google.protobuf.EnumValue
import com.google.protobuf.GeneratedMessageV3
import io.spine.base.EventMessage
import io.spine.protobuf.AnyPacker
import io.spine.protobuf.TypeConverter
import io.spine.protodata.Option
import java.util.*

/**
 * Yields events regarding a set of options.
 *
 * @param options
 *     the set of options, such as `FileOptions`, `FieldOptions`, etc.
 * @param ctor
 *     a function which given an option, constructs a fitting event
 */
internal suspend fun SequenceScope<EventMessage>.produceOptionEvents(
    options: GeneratedMessageV3.ExtendableMessage<*>,
    ctor: (Option) -> EventMessage
) {
    options.allFields.forEach { (optionDescriptor, value) ->
        if(value is Collection<*>) {
            value.forEach {
                val option = toOption(optionDescriptor, it!!)
                yield(ctor(option))
            }
        } else {
            val option = toOption(optionDescriptor, value)
            yield(ctor(option))
        }
    }
}

private fun toOption(
    optionDescriptor: FieldDescriptor,
    value: Any
): Option {
    val optionValue = fieldToAny(optionDescriptor, value)
    val option = Option.newBuilder()
        .setName(optionDescriptor.name)
        .setNumber(optionDescriptor.number)
        .setType(optionDescriptor.type())
        .setValue(optionValue)
        .build()
    return option
}

private fun fieldToAny(field: FieldDescriptor, value: Any): com.google.protobuf.Any =
    if (field.type == ENUM) {
        val enumValue = value as EnumValueDescriptor
        AnyPacker.pack(EnumValue.newBuilder()
                                .setName(enumValue.name)
                                .setNumber(enumValue.number)
                                .build())
    } else {
        TypeConverter.toAny(value)
    }
