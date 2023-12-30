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

@file:JvmName("Values")

package io.spine.protodata

import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.BOOLEAN
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.BYTE_STRING
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.DOUBLE
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.ENUM
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.FLOAT
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.INT
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.LONG
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING
import com.google.protobuf.MapEntry
import com.google.protobuf.Message
import com.google.protobuf.StringValue
import io.spine.protobuf.pack
import io.spine.protodata.MapValueKt.entry
import io.spine.protodata.NullValue.NULL_VALUE

import com.google.protobuf.Any as ProtoAny

/**
 * Converts this message to a [Value] instance.
 *
 * If the message is equal to the default instance, it will be represented by
 * a `MessageValue` with no fields. Otherwise, all the present fields are converted
 * into `Value`s.
 */
public fun Message.toValue(): Value = value {
    messageValue = messageValue {
        type = descriptorForType.name()
        allFields.forEach { (k, v) ->
            fields.put(k.name, k.toValue(v))
        }
    }
}

/**
 * Constructs a [Value] instance which corresponds to the raw value of the field.
 */
private fun FieldDescriptor.toValue(raw: Any): Value = when {
    isMapField -> fromMap(raw)
    isRepeated -> fromList(raw)
    else -> singularValue(raw)
}

private fun FieldDescriptor.singularValue(raw: Any) = when (javaType) {
    INT -> value { intValue = (raw as Int).toLong() }
    LONG -> value { intValue = raw as Long }
    FLOAT -> value { doubleValue = (raw as Float).toDouble() }
    DOUBLE -> value { doubleValue = raw as Double }
    BOOLEAN -> value { boolValue = raw as Boolean }
    STRING -> value { stringValue = raw as String }
    BYTE_STRING -> value { bytesValue = raw as ByteString }
    ENUM -> value {
        val enumDescriptor = raw as EnumValueDescriptor
        enumValue = enumValue {
            type = enumType.name()
            constNumber = enumDescriptor.number
        }
    }
    MESSAGE -> (raw as Message).toValue()
    else -> NULL
}

private fun FieldDescriptor.fromMap(rawValue: Any): Value {
    val syntheticEntry = messageType.fields
    val keyType = syntheticEntry[0]
    val valueType = syntheticEntry[1]
    @Suppress("UNCHECKED_CAST")
    val entries = rawValue as List<MapEntry<*, *>>
    return value {
        mapValue = mapValue {
            value.addAll(entries.toMapValueEntries(keyType, valueType))
        }
    }
}

private fun List<MapEntry<*, *>>.toMapValueEntries(
    keyType: FieldDescriptor,
    valueType: FieldDescriptor
): Iterable<MapValue.Entry> = map {
    entry {
        key = keyType.toValue(it.key)
        value = valueType.toValue(it.value)
    }
}

private fun FieldDescriptor.fromList(raw: Any): Value = value {
    val list = raw as List<*>
    listValue = listValue {
        values.addAll(list.map { entry -> singularValue(entry!!) })
    }
}

/**
 * The value of `null`.
 */
public val NULL: Value by lazy {
    value { nullValue = NULL_VALUE }
}

/**
 * `true` wrapped into `BoolValue` and packed.
 */
public val packedTrue: ProtoAny by lazy {
    BoolValue.of(true).pack()
}

/**
 * `false` wrapped into `BoolValue` and packed.
 */
public val packedFalse: ProtoAny by lazy {
    BoolValue.of(false).pack()
}

/**
 * Wraps this string into `StringValue` and packs.
 */
public fun String.pack(): ProtoAny =
    StringValue.of(this).pack()
