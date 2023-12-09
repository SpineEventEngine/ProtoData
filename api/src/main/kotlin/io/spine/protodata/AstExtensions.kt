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

@file:JvmName("Ast")

@file:Suppress("TooManyFunctions")

package io.spine.protodata

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.Message
import io.spine.option.OptionsProto
import io.spine.protodata.CallCardinality.BIDIRECTIONAL_STREAMING
import io.spine.protodata.CallCardinality.CLIENT_STREAMING
import io.spine.protodata.CallCardinality.SERVER_STREAMING
import io.spine.protodata.CallCardinality.UNARY

/**
 * Obtains a name of this Protobuf file without the extension.
 *
 * @receiver the header of the Protobuf file of interest.
 */
public fun ProtoFileHeader.nameWithoutExtension(): String {
    val name = file.path.split("/").last()
    val index = name.indexOf(".")
    return if (index > 0) {
        name.substring(0, index)
    } else {
        name
    }
}

/**
 * Obtains the package and the name of the type.
 */
public val MessageType.qualifiedName: String
    get() = name.qualifiedName

/**
 * Obtains the fully qualified name from this `TypeName`.
 */
public val TypeNameOrBuilder.qualifiedName: String
    get() {
        val names = buildList<String> {
            add(packageName)
            addAll(nestingTypeNameList)
            add(simpleName)
        }
        return names.filter { it.isNotEmpty() }.joinToString(separator = ".")
    }

/**
 * Tells if this field is a Protobuf message.
 */
public val Field.isMessage: Boolean
    get() = type.hasMessage()

/**
 * Shows if this field is a `map`.
 *
 * If the field is a `map`, the `Field.type` contains the type of the value, and
 * the `Field.map.key_type` contains the type the map key.
 */
public val Field.isMap: Boolean
    get() = hasMap()

/**
 * Shows if this field is a list.
 *
 * In Protobuf `repeated` keyword denotes a sequence of values for a field.
 * However, a map is also treated as a repeated field for serialization reasons.
 * We use the term "list" for repeated fields which are not maps.
 */
public val Field.isList: Boolean
    get() = hasList()

/**
 * Shows if this field repeated.
 *
 * Can be declared in Protobuf either as a `map` or a `repeated` field.
 */
public val Field.isRepeated: Boolean
    get() = isMap || isList

/**
 * Shows if this field is a part of a `oneof` group.
 *
 * If the field is a part of a `oneof`, the `Field.oneof_name` contains the name of that `oneof`.
 */
public val Field.isPartOfOneof: Boolean
    get() = hasOneofName()

/**
 * The field name containing a qualified name of the declaring type.
 */
public val Field.qualifiedName: String
    get() = "${declaringType.qualifiedName}.${name.value}"

/**
 * Looks up an option value by the [optionName].
 *
 * If the option has a Protobuf primitive type, [cls] must be the wrapper type.
 * For example, an `Int32Value` for `int32`, `StringValue` for `string`, etc.
 *
 * @return the value of the option or a `null` if the option is not found.
 */
public fun <T : Message> Iterable<Option>.find(optionName: String, cls: Class<T>): T? {
    val value = firstOrNull { it.name == optionName }?.value
    return value?.unpack(cls)
}

/**
 * Obtains the name of this message type as a [TypeName].
 */
public fun Descriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Obtains the name of this enum type as a [TypeName].
 */
public fun EnumDescriptor.name(): TypeName = buildTypeName(name, file, containingType)

private fun buildTypeName(
    simpleName: String,
    file: FileDescriptor,
    containingDeclaration: Descriptor?
): TypeName {
    val nestingNames = mutableListOf<String>()
    var parent = containingDeclaration
    while (parent != null) {
        nestingNames.add(0, parent.name)
        parent = parent.containingType
    }
    val typeName = TypeName.newBuilder()
        .setSimpleName(simpleName)
        .setPackageName(file.`package`)
        .setTypeUrlPrefix(file.typeUrlPrefix)
    if (nestingNames.isNotEmpty()) {
        typeName.addAllNestingTypeName(nestingNames)
    }
    return typeName.build()
}

private val FileDescriptor.typeUrlPrefix: String
    get() {
        val customTypeUrl = options.getExtension(OptionsProto.typeUrlPrefix)
        return if (customTypeUrl.isNullOrBlank()) {
            "type.googleapis.com"
        } else {
            customTypeUrl
        }
    }

/**
 * Obtains the name of this `oneof` as a [OneofName].
 */
public fun OneofDescriptor.name(): OneofName = oneofName { value = name }

/**
 * Obtains the name of this field as a [FieldName].
 */
public fun FieldDescriptor.name(): FieldName = fieldName { value = name }

/**
 * Obtains the relative path to this file as a [File].
 */
public fun FileDescriptor.file(): File = file { path = name }

/**
 * Obtains the name of this service as a [ServiceName].
 */
public fun ServiceDescriptor.name(): ServiceName = serviceName {
    typeUrlPrefix = file.typeUrlPrefix
    packageName = file.`package`
    simpleName = name
}

/**
 * Obtains the name of this RPC method as an [RpcName].
 */
public fun MethodDescriptor.name(): RpcName = rpcName { value = name }

/**
 * Obtains a [Type] wrapping this `PrimitiveType`.
 */
public fun PrimitiveType.asType(): Type = type { primitive = this@asType }

/**
 * Obtains the [CallCardinality] of this RPC method.
 *
 * The cardinality determines how many messages may flow from the client to the server and back.
 */
public val MethodDescriptor.cardinality: CallCardinality
    get() = when {
        !isClientStreaming && !isServerStreaming -> UNARY
        !isClientStreaming && isServerStreaming -> SERVER_STREAMING
        isClientStreaming && !isServerStreaming -> CLIENT_STREAMING
        isClientStreaming && isServerStreaming -> BIDIRECTIONAL_STREAMING
        else -> error("Unable to determine cardinality of method: `$fullName`.")
    }

/**
 * Tells if this type is a Protobuf primitive type.
 */
public val Type.isPrimitive: Boolean
    get() = hasPrimitive()

/**
 * Tells if this type represents a Protobuf message.
 */
public val Type.isMessage: Boolean
    get() = hasMessage()

/**
 * Tells if this type is `google.protobuf.Any`.
 */
public val Type.isAny: Boolean
    get() = isMessage
            && message.packageName.equals("google.protobuf")
            && message.simpleName.equals("Any")
