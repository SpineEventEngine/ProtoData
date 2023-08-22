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

package io.spine.protodata.backend

import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors
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
import io.spine.protobuf.isNotDefault
import io.spine.protodata.EnumValue
import io.spine.protodata.MapValue
import io.spine.protodata.MessageValue
import io.spine.protodata.NullValue.NULL_VALUE
import io.spine.protodata.Value
import io.spine.protodata.listValue
import io.spine.protodata.mapValue
import io.spine.protodata.name
import io.spine.protodata.value

/**
 * A factory of `Value`s.
 */
public object Values {

    private val NULL = value { nullValue = NULL_VALUE }

    /**
     * Converts the given message into a value.
     *
     * If the message is equal to the default instance, it will be represented by
     * a `MessageValue` with no fields. Otherwise, all the present fields are converted
     * into `Value`s.
     */
    @JvmStatic
    public fun from(message: Message): Value {
        val builder = MessageValue.newBuilder()
            .setType(message.descriptorForType.name())
        if (message.isNotDefault()) {
            populate(builder, message)
        }
        return value {
            messageValue = builder.build()
        }
    }

    private fun populate(value: MessageValue.Builder, source: Message) {
        source.allFields.forEach { (k, v) ->
            value.putFields(k.name, fromField(k, v))
        }
    }

    private fun fromField(
        field: FieldDescriptor,
        value: Any
    ): Value {
        return if (field.isMapField) {
            fromMap(field, value)
        } else if (field.isRepeated) {
            fromList(field, value)
        } else {
            singularValue(value, field)
        }
    }

    private fun singularValue(raw: Any, field: FieldDescriptor) = when (field.javaType) {
        INT -> value { intValue = (raw as Int).toLong() }
        LONG -> value { intValue = raw as Long }
        FLOAT -> value { doubleValue = (raw as Float).toDouble() }
        DOUBLE -> value { doubleValue = raw as Double }
        BOOLEAN -> value { boolValue = raw as Boolean }
        STRING -> value { stringValue = raw as String }
        BYTE_STRING -> value { bytesValue = raw as ByteString }
        ENUM -> value {
            val enumDescriptor = raw as EnumValueDescriptor
            enumValue = EnumValue.newBuilder()
                .setType(field.enumType.name())
                .setConstNumber(enumDescriptor.number)
                .build()
        }

        MESSAGE -> from(raw as Message)
        else -> NULL
    }

    private fun fromMap(field: FieldDescriptor, rawValue: Any): Value {
        val syntheticEntry = field.messageType
            .fields
        val keyType = syntheticEntry[0]
        val valueType = syntheticEntry[1]
        @Suppress("UNCHECKED_CAST")
        val entries = rawValue as List<MapEntry<*, *>>
        return value {
            this.mapValue = mapValue {
                value.addAll(entries.map {
                    val key = fromField(keyType, it.key!!)
                    val packedValue: Value = fromField(valueType, it.value!!)
                    MapValue.Entry.newBuilder()
                        .setKey(key)
                        .setValue(packedValue)
                        .build()
                })
            }
        }
    }

    private fun fromList(field: FieldDescriptor, value: Any): Value {
        val list = value as List<*>
        return value {
            listValue = listValue {
                values.addAll(list.map { entry -> singularValue(entry!!, field) })
            }
        }
    }
}
