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
import com.google.protobuf.Descriptors.FileDescriptor
import io.spine.protodata.ProtoFileHeader.SyntaxVersion
import io.spine.protodata.PrimitiveType
import io.spine.protodata.PrimitiveType.TYPE_BOOL
import io.spine.protodata.PrimitiveType.TYPE_BYTES
import io.spine.protodata.PrimitiveType.TYPE_DOUBLE
import io.spine.protodata.PrimitiveType.TYPE_FIXED32
import io.spine.protodata.PrimitiveType.TYPE_FIXED64
import io.spine.protodata.PrimitiveType.TYPE_FLOAT
import io.spine.protodata.PrimitiveType.TYPE_INT32
import io.spine.protodata.PrimitiveType.TYPE_INT64
import io.spine.protodata.PrimitiveType.TYPE_SFIXED32
import io.spine.protodata.PrimitiveType.TYPE_SFIXED64
import io.spine.protodata.PrimitiveType.TYPE_SINT32
import io.spine.protodata.PrimitiveType.TYPE_SINT64
import io.spine.protodata.PrimitiveType.TYPE_STRING
import io.spine.protodata.PrimitiveType.TYPE_UINT32
import io.spine.protodata.PrimitiveType.TYPE_UINT64
import io.spine.protodata.Type
import io.spine.protodata.type
import io.spine.protodata.asType
import io.spine.protodata.name

/**
 * Constructs a [Type] of the receiver field.
 */
internal fun FieldDescriptor.type(): Type {
    return when (type) {
        ENUM -> enum(this)
        MESSAGE -> message(this)
        GROUP -> error("Cannot process the field `$fullName` of type `$type`.")
        else -> primitiveType().asType()
    }
}

/**
 * Obtains the type of this field as a [PrimitiveType] or throws an exception if the type is not
 * a primitive type.
 */
@Suppress("ComplexMethod") // ... not really, performing plain conversion.
internal fun FieldDescriptor.primitiveType(): PrimitiveType =
    when (type) {
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
        else -> error("`$type` is not a primitive type.")
    }

/**
 * Obtains the type of the given [field] as an enum type.
 */
private fun enum(field: FieldDescriptor): Type = type {
    enumeration = field.enumType.name()
}

/**
 * Obtains the type of the given [field] as a message type.
 */
private fun message(field: FieldDescriptor): Type = type {
    message = field.messageType.name()
}

/**
 * Obtains the syntax version of the given [FileDescriptor].
 */
internal fun FileDescriptor.syntaxVersion(): SyntaxVersion =
    when (toProto().syntax) {
        "proto2" -> SyntaxVersion.PROTO2
        "proto3" -> SyntaxVersion.PROTO3
        else -> SyntaxVersion.PROTO2
    }
