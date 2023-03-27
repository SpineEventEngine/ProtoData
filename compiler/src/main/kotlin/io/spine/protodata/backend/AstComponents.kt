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

import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Empty
import io.spine.protodata.EnumConstant
import io.spine.protodata.Field
import io.spine.protodata.Rpc
import io.spine.protodata.ServiceName
import io.spine.protodata.TypeName
import io.spine.protodata.cardinality
import io.spine.protodata.constantName
import io.spine.protodata.file
import io.spine.protodata.name
import io.spine.protodata.path

/**
 * Converts this field descriptor into a [Field] with options.
 *
 * @see buildField
 */
internal fun FieldDescriptor.buildFieldWithOptions(
    declaringType: TypeName,
    documentation: Documentation
): Field {
    val field = buildField(declaringType, documentation)
    return field.toBuilder()
        .addAllOption(listOptions(options))
        .build()
}

/**
 * Converts this field descriptor into a [Field].
 *
 * The resulting [Field] will not reflect the field options.
 *
 * @see buildFieldWithOptions
 */
internal fun FieldDescriptor.buildField(
    declaringType: TypeName,
    documentation: Documentation
): Field {
    return Field.newBuilder()
        .setName(name())
        .setDeclaringType(declaringType)
        .setNumber(number)
        .setOrderOfDeclaration(index)
        .copyTypeAndCardinality(this)
        .setDoc(documentation.forField(this))
        .build()
}

/**
 * Copies the field type and cardinality (`map`/`list`/`oneof_name`/`single`) from
 * the given descriptor to the receiver builder.
 *
 * @return the receiver for method chaining
 */
private fun Field.Builder.copyTypeAndCardinality(
    desc: FieldDescriptor
): Field.Builder {
    if (desc.isMapField) {
        val (keyField, valueField) = desc.messageType.fields
        map = Field.OfMap.newBuilder()
            .setKeyType(keyField.primitiveType())
            .build()
        type = valueField.type()
    } else {
        type = desc.type()
        when {
            desc.isRepeated -> list = Empty.getDefaultInstance()
            desc.realContainingOneof != null -> oneofName = desc.realContainingOneof.name()
            else -> single = Empty.getDefaultInstance()
        }
    }
    return this
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
    val constant = buildConstant(declaringType, documentation)
    return constant.toBuilder()
        .addAllOption(listOptions(options))
        .build()
}

/**
 * Converts this enum value descriptor into an [EnumConstant].
 *
 * The resulting [EnumConstant] will not reflect the options on the enum constant.
 *
 * @see buildConstantWithOptions
 */
internal fun EnumValueDescriptor.buildConstant(
    declaringType: TypeName,
    documentation: Documentation
): EnumConstant {
    return EnumConstant.newBuilder()
        .setName(constantName { value = name })
        .setDeclaredIn(declaringType)
        .setNumber(number)
        .setOrderOfDeclaration(index)
        .setDoc(documentation.forEnumConstant(this))
        .build()
}

/**
 * Converts this method descriptor into an [Rpc] with options.
 *
 * @see buildRpc
 */
internal fun MethodDescriptor.buildRpcWithOptions(
    declaringService: ServiceName,
    documentation: Documentation
) : Rpc {
    return buildRpc(declaringService, documentation)
        .toBuilder()
        .addAllOption(listOptions(options))
        .build()
}

/**
 * Converts this method descriptor into an [Rpc].
 *
 * The resulting [Rpc] will not reflect the method options.
 *
 * @see buildRpcWithOptions
 */
internal fun MethodDescriptor.buildRpc(
    declaringService: ServiceName,
    documentation: Documentation
) : Rpc {
    val name = name()
    val cardinality = cardinality()
    return Rpc.newBuilder()
        .setName(name)
        .setCardinality(cardinality)
        .setRequestType(inputType.name())
        .setResponseType(outputType.name())
        .setDoc(documentation.forRpc(this))
        .setService(declaringService)
        .build()
}

/**
 * Extracts metadata from this file descriptor, including file options.
 *
 * @see toFile
 */
internal fun FileDescriptor.toFileWithOptions() =
    toFile()
        .toBuilder()
        .addAllOption(listOptions(options))
        .build()

/**
 * Extracts metadata from this file descriptor, excluding file options.
 *
 * @see toFileWithOptions
 */
internal fun FileDescriptor.toFile() = file {
    path = path()
    packageName = `package`
    syntax = this@toFile.syntax.toSyntaxVersion()
}
