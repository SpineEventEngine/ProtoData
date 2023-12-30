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

package io.spine.protodata.backend.event

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Empty
import io.spine.protodata.EnumConstant
import io.spine.protodata.Field
import io.spine.protodata.FieldKt
import io.spine.protodata.FieldKt.ofMap
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.Rpc
import io.spine.protodata.ServiceName
import io.spine.protodata.TypeName
import io.spine.protodata.backend.Documentation
import io.spine.protodata.backend.primitiveType
import io.spine.protodata.backend.syntaxVersion
import io.spine.protodata.backend.type
import io.spine.protodata.cardinality
import io.spine.protodata.constantName
import io.spine.protodata.copy
import io.spine.protodata.enumConstant
import io.spine.protodata.field
import io.spine.protodata.file
import io.spine.protodata.name
import io.spine.protodata.protoFileHeader
import io.spine.protodata.rpc

/**
 * Converts this field descriptor into a [Field] with options.
 *
 * @see buildField
 */
internal fun FieldDescriptor.buildFieldWithOptions(
    declaringType: TypeName,
    documentation: Documentation
): Field {
    val field = buildField(this, declaringType, documentation)
    return field.copy {
        // There are several similar expressions in this file. Sadly, they have no common
        // compile-time type that would allow us to extract the duplicates into a function.
        option.addAll(options.toList())
    }
}

/**
 * Converts this field descriptor into a [Field].
 *
 * The resulting [Field] will not reflect the field options.
 *
 * @see buildFieldWithOptions
 */
internal fun buildField(
    desc: FieldDescriptor,
    declaredIn: TypeName,
    documentation: Documentation
) = field {
    name = desc.name()
    orderOfDeclaration = desc.index
    doc = documentation.forField(desc)
    number = desc.number
    declaringType = declaredIn
    copyTypeAndCardinality(desc)
}

/**
 * Copies the field type and cardinality (`map`/`list`/`oneof_name`/`single`) from
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
 * Converts this enum value descriptor into an [EnumConstant] with options.
 *
 * @see buildConstant
 */
internal fun EnumValueDescriptor.buildConstantWithOptions(
    declaringType: TypeName,
    documentation: Documentation
): EnumConstant {
    val constant = buildConstant(this, declaringType, documentation)
    return constant.copy {
        option.addAll(options.toList())
    }
}

/**
 * Converts this enum value descriptor into an [EnumConstant].
 *
 * The resulting [EnumConstant] will not reflect the options on the enum constant.
 *
 * @see buildConstantWithOptions
 */
internal fun buildConstant(
    desc: EnumValueDescriptor,
    declaringType: TypeName,
    documentation: Documentation
) = enumConstant {
    name = constantName { value = desc.name }
    declaredIn = declaringType
    number = desc.number
    orderOfDeclaration = desc.index
    doc = documentation.forEnumConstant(desc)
}

/**
 * Converts this method descriptor into an [Rpc] with options.
 *
 * @see buildRpc
 */
internal fun MethodDescriptor.buildRpcWithOptions(
    declaringService: ServiceName,
    documentation: Documentation
): Rpc {
    val rpc = buildRpc(this, declaringService, documentation)
    return rpc.copy {
        option.addAll(options.toList())
    }
}

/**
 * Converts this method descriptor into an [Rpc].
 *
 * The resulting [Rpc] will not reflect the method options.
 *
 * @see buildRpcWithOptions
 */
internal fun buildRpc(
    desc: MethodDescriptor,
    declaringService: ServiceName,
    documentation: Documentation
) = rpc {
    name = desc.name()
    cardinality = desc.cardinality
    requestType = desc.inputType.name()
    responseType = desc.outputType.name()
    doc = documentation.forRpc(desc)
    service = declaringService
}

/**
 * Extracts metadata from this file descriptor, including file options.
 */
internal fun FileDescriptor.toHeader(): ProtoFileHeader = protoFileHeader {
    file = file()
    packageName = `package`
    syntax = syntaxVersion()
    option.addAll(options.toList())
}

/**
 * Obtains the file path from this file descriptor.
 */
internal fun FileDescriptorProto.toFile() = file {
    path = name
}
