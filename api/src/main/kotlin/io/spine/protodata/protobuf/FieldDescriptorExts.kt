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

package io.spine.protodata.protobuf

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type.BOOL
import com.google.protobuf.Descriptors.FieldDescriptor.Type.BYTES
import com.google.protobuf.Descriptors.FieldDescriptor.Type.DOUBLE
import com.google.protobuf.Descriptors.FieldDescriptor.Type.ENUM
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FIXED32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FIXED64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.FLOAT
import com.google.protobuf.Descriptors.FieldDescriptor.Type.GROUP
import com.google.protobuf.Descriptors.FieldDescriptor.Type.INT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.INT64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.MESSAGE
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SFIXED32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SFIXED64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SINT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.SINT64
import com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING
import com.google.protobuf.Descriptors.FieldDescriptor.Type.UINT32
import com.google.protobuf.Descriptors.FieldDescriptor.Type.UINT64
import com.google.protobuf.Empty
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldKt
import io.spine.protodata.ast.FieldKt.ofMap
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.PrimitiveType
import io.spine.protodata.ast.PrimitiveType.TYPE_BOOL
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.ast.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_SINT64
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT32
import io.spine.protodata.ast.PrimitiveType.TYPE_UINT64
import io.spine.protodata.ast.Type
import io.spine.protodata.ast.copy
import io.spine.protodata.ast.field
import io.spine.protodata.ast.fieldName
import io.spine.protodata.ast.toList
import io.spine.protodata.ast.toType
import io.spine.protodata.ast.type

/**
 * Obtains the name of this field as a [FieldName].
 */
public fun FieldDescriptor.name(): FieldName = fieldName { value = name }

/**
 * Converts this field descriptor into a [Field] with options.
 *
 * @see buildField
 */
public fun FieldDescriptor.toField(): Field {
    val field = buildField(this)
    return field.copy {
        // There are several similar expressions in this file like
        // the `option.addAll()` call below. Sadly, these duplicates
        // could not be refactored into a common function because
        // they have no common compile-time type.
        option.addAll(options.toList())
    }
}

/**
 * Converts this field descriptor into a [Field].
 *
 * The resulting [Field] will not reflect the field options.
 *
 * @see toField
 */
public fun buildField(desc: FieldDescriptor): Field =
    field {
        val declaredIn = desc.containingType.name()
        name = desc.name()
        orderOfDeclaration = desc.index
        doc = desc.fileDoc.forField(desc)
        number = desc.number
        declaringType = declaredIn
        copyTypeAndCardinality(desc)
    }

/**
 * Converts the field type and cardinality (`map`/`list`/`oneof_name`/`single`) from
 * the given descriptor to the receiver DSL-style builder.
 */
private fun FieldKt.Dsl.copyTypeAndCardinality(
    desc: FieldDescriptor
) {
    if (desc.isMapField) {
        val (keyField, valueField) = desc.messageType.fields
        map = ofMap { keyType = keyField.primitiveType() }
        type = valueField.type()
    } else {
        type = desc.type()
        when {
            desc.isRepeated -> list = Empty.getDefaultInstance()
            desc.realContainingOneof != null -> oneofName = desc.realContainingOneof.name()
            else -> single = Empty.getDefaultInstance()
        }
    }
}

/**
 * Constructs a [Type] of the receiver field.
 */
public fun FieldDescriptor.type(): Type {
    return when (type) {
        ENUM -> enum(this)
        MESSAGE -> message(this)
        GROUP -> error("Cannot process the field `$fullName` of type `$type`.")
        else -> primitiveType().toType()
    }
}

/**
 * Obtains the type of the given [field] as a message type.
 */
private fun message(field: FieldDescriptor): Type = type {
    message = field.messageType.name()
}

/**
 * Converts this field type into an instance of [PrimitiveType], or
 * throws an exception if this type is not primitive.
 */
@Suppress("ComplexMethod") // ... not really, performing plain conversion.
public fun FieldDescriptor.Type.toPrimitiveType(): PrimitiveType =
    when (this) {
        BOOL -> TYPE_BOOL
        BYTES -> TYPE_BYTES
        DOUBLE -> TYPE_DOUBLE
        FIXED32 -> TYPE_FIXED32
        FIXED64 -> TYPE_FIXED64
        FLOAT -> TYPE_FLOAT
        INT32 -> TYPE_INT32
        INT64 -> TYPE_INT64
        SFIXED32 -> TYPE_SFIXED32
        SFIXED64 -> TYPE_SFIXED64
        SINT32 -> TYPE_SINT32
        SINT64 -> TYPE_SINT64
        STRING -> TYPE_STRING
        UINT32 -> TYPE_UINT32
        UINT64 -> TYPE_UINT64
        else -> error("`$this` is not a primitive type.")
    }

/**
 * Obtains the type of this field as a [PrimitiveType] or throws an exception
 * if the type is not primitive.
 */
public fun FieldDescriptor.primitiveType(): PrimitiveType = type.toPrimitiveType()

/**
 * Obtains the type of the given [field] as an enum type.
 */
private fun enum(field: FieldDescriptor): Type = type {
    enumeration = field.enumType.name()
}

/**
 * Transforms this `Iterable` of field descriptors into an `Iterable` with [Field] instances.
 */
internal fun Iterable<FieldDescriptor>.mapped(): Iterable<Field> = map { it.toField() }
