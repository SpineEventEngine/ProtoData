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

package io.spine.protodata.ast

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type.ENUM
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage
import com.google.protobuf.Message
import io.spine.base.EventMessage
import io.spine.option.OptionsProto
import io.spine.protobuf.TypeConverter
import io.spine.protobuf.defaultInstance
import io.spine.protobuf.pack
import io.spine.protobuf.unpack
import io.spine.protodata.protobuf.name
import io.spine.protodata.protobuf.type
import com.google.protobuf.Any as ProtoAny

/**
 * Unpacks the value of this option using the specified generic type.
 */
public inline fun <reified T : Message> Option.unpack(): T =
    value.unpack<T>()

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
 * Looks up an option with the given type [T].
 *
 * @return the value of the option or `null` if the option is not found.
 */
public inline fun <reified T : Message> Iterable<Option>.find(): T? {
    val cls = T::class.java
    val typeName = cls.defaultInstance.descriptorForType.name()
    val found = filter { it.type.isMessage }.firstOrNull { it.type.message == typeName }
    val result = found?.unpack<T>()
    return result
}

/**
 * Yields events regarding a set of options.
 *
 * @param options The set of options, such as `FileOptions`, `FieldOptions`, etc.
 * @param factory A function which given an option, constructs a fitting event.
 */
public suspend fun SequenceScope<EventMessage>.produceOptionEvents(
    options: ExtendableMessage<*>,
    context: GenericDescriptor,
    factory: (Option) -> EventMessage
) {
    val parsed = options.parseOptions(context)
    parsed.forEach {
        yield(factory(it))
    }
}

/**
 * Obtains options declared in this file.
 */
public fun FileDescriptor.options(): List<Option> = options.toList(this)

/**
 * Obtains options declared in this message.
 */
public fun Descriptor.options(): List<Option> = options.toList(this)

/**
 * Obtains options declared in this enumeration.
 */
public fun EnumDescriptor.options(): List<Option> = options.toList(this)

/**
 * Obtains options declared in this service.
 */
public fun ServiceDescriptor.options(): List<Option> = options.toList(this)

/**
 * Obtains options declared in this field.
 */
public fun FieldDescriptor.options(): List<Option> = options.toList(this)

/**
 * Obtains options declared in this oneof.
 */
public fun OneofDescriptor.options(): List<Option> = options.toList(this)

/**
 * Obtains options declared in this enum item.
 */
public fun EnumValueDescriptor.options(): List<Option> = options.toList(this)

/**
 * Obtains options declared in this `rpc` method.
 */
public fun MethodDescriptor.options(): List<Option> = options.toList(this)

/**
 * Parses this `options` message into a list of [Option]s.
 */
private fun ExtendableMessage<*>.toList(context: GenericDescriptor): List<Option> =
    parseOptions(context).toList()

private fun ExtendableMessage<*>.parseOptions(context: GenericDescriptor): Sequence<Option> =
    sequence {
        allFields.forEach { (optionDescriptor, value) ->
            if (value is Collection<*>) {
                value.forEach { item ->
                    val option = optionDescriptor.toOption(item!!, context)
                    yield(option)
                }
            } else {
                val option = optionDescriptor.toOption(value, context)
                yield(option)
            }
        }
    }

private fun FieldDescriptor.toOption(value: Any, context: GenericDescriptor): Option {
    val optionDescriptor = this
    val optionValue = optionDescriptor.packOptionValue(value)
    val option = option {
        name = optionDescriptor.name
        number = optionDescriptor.number
        type = optionDescriptor.type()
        this.value = optionValue

        doc = context.file.documentation().forOption(optionDescriptor, context)
        span = context.file.coordinates().forOption(optionDescriptor, context)
    }
    return option
}

private fun FieldDescriptor.packOptionValue(value: Any): ProtoAny =
    if (type == ENUM) {
        val descr = value as EnumValueDescriptor
        val enumValue = com.google.protobuf.enumValue {
            name = descr.name
            number = descr.number
        }
        enumValue.pack()
    } else {
        TypeConverter.toAny(value)
    }
