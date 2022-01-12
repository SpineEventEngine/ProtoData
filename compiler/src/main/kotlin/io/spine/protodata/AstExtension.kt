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

@file:JvmName("Ast")

package io.spine.protodata

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.Message
import io.spine.option.OptionsProto
import io.spine.protodata.CallCardinality.BIDIRECTIONAL_STREAMING
import io.spine.protodata.CallCardinality.CLIENT_STREAMING
import io.spine.protodata.CallCardinality.SERVER_STREAMING
import io.spine.protodata.CallCardinality.UNARY

/**
 * Obtains the package and the name of the type.
 */
public fun MessageType.qualifiedName(): String = name.qualifiedName()

/**
 * Obtains the type URl of the type.
 *
 * A type URL contains the type URL prefix and the qualified name of the type separated by
 * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
 *
 * @see MessageType.qualifiedName
 * @see TypeName.typeUrl
 */
public fun MessageType.typeUrl(): String = name.typeUrl()

/**
 * Obtains the type URl of the type.
 *
 * A type URL contains the type URL prefix and the qualified name of the type separated by
 * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
 *
 * @see MessageType.qualifiedName
 * @see TypeName.typeUrl
 */
public fun EnumType.typeUrl(): String = name.typeUrl()

/**
 * Obtains the fully qualified name from this `TypeName`.
 */
public fun TypeName.qualifiedName(): String {
    val names = mutableListOf<String>()
    names.add(packageName)
    names.addAll(nestingTypeNameList)
    names.add(simpleName)
    return names
        .filter { it.isNotEmpty() }
        .joinToString(separator = ".")
}

/**
 * Obtains the type URL from this `TypeName`.
 *
 * A type URL contains the type URL prefix and the qualified name of the type separated by
 * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
 *
 * @see TypeName.qualifiedName
 * @see MessageType.typeUrl
 */
public fun TypeName.typeUrl(): String = "${typeUrlPrefix}/${qualifiedName()}"

/**
 * Obtains the type URl from this `ServiceName`.
 *
 * A type URL contains the type URL prefix and the qualified name of the type separated by
 * the slash (`/`) symbol. See the docs of `google.protobuf.Any.type_url` for more info.
 */
public fun ServiceName.typeUrl(): String = "$typeUrlPrefix/$packageName.$simpleName"

/**
 * Obtains the type URl of this service.
 *
 * @see ServiceName.typeUrl
 */
public fun Service.typeUrl(): String = name.typeUrl()

/**
 * Shows if this field is a `map`.
 *
 * If the field is a `map`, the `Field.type` contains the type of the value, and
 * the `Field.map.key_type` contains the type the the map key.
 */
public fun Field.isMap(): Boolean = hasMap()

/**
 * Shows if this field is a list.
 *
 * In Protobuf `repeated` keyword denotes a sequence of values for a field. However, a map is also
 * treated as a repeated field for serialization reasons. We use the term "list" for repeated fields
 * which are not maps.
 */
public fun Field.isList(): Boolean = hasList()

/**
 * Shows if this field repeated.
 *
 * Can be declared in Protobuf either as a `map` or a `repeated` field.
 */
public fun Field.isRepeated(): Boolean = isMap() || isList()

/**
 * Shows if this field is a part of a `oneof` group.
 *
 * If the field is a part of a `oneof`, the `Field.oneof_name` contains the name of that `oneof`.
 */
public fun Field.isPartOfOneof(): Boolean = hasOneofName()

/**
 * Looks up an option value by the [optionName].
 *
 * If the option has a Protobuf primitive type, [cls] must be the wrapper type. For example,
 * an `Int32Value` for `int32`, `StringValue` for `string`, etc.
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

private fun buildTypeName(simpleName: String,
                          file: FileDescriptor,
                          containingDeclaration: Descriptor?): TypeName {
    val nestingNames = mutableListOf<String>()
    var parent = containingDeclaration
    while (parent != null) {
        nestingNames.add(0, parent.name)
        parent = parent.containingType
    }
    val typeName = TypeName
        .newBuilder()
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
internal fun OneofDescriptor.name(): OneofName =
    OneofName.newBuilder()
             .setValue(name)
             .build()

/**
 * Obtains the name of this field as a [FieldName].
 */
internal fun FieldDescriptor.name(): FieldName =
    FieldName.newBuilder()
             .setValue(name)
             .build()

/**
 * Obtains the relative path to this file as a [FilePath].
 */
internal fun FileDescriptor.path(): FilePath =
    FilePath.newBuilder()
            .setValue(name)
            .build()

/**
 * Obtains the name of this service as a [ServiceName].
 */
internal fun ServiceDescriptor.name(): ServiceName =
    ServiceName.newBuilder()
               .setTypeUrlPrefix(file.typeUrlPrefix)
               .setPackageName(file.`package`)
               .setSimpleName(name)
               .build()

/**
 * Obtains the name of this RPC method as an [RpcName].
 */
internal fun Descriptors.MethodDescriptor.name(): RpcName =
    RpcName.newBuilder()
           .setValue(name)
           .build()

/**
 * Obtains a [Type] wrapping this `PrimitiveType`.
 */
internal fun PrimitiveType.asType(): Type =
    Type.newBuilder()
        .setPrimitive(this)
        .build()

/**
 * Obtains the [CallCardinality] of this RPC method.
 *
 * The cardinality determines how many messages may flow from the client to the server and back.
 */
internal fun Descriptors.MethodDescriptor.cardinality(): CallCardinality =
    when {
        !isClientStreaming && !isServerStreaming -> UNARY
        !isClientStreaming && isServerStreaming -> SERVER_STREAMING
        isClientStreaming && !isServerStreaming -> CLIENT_STREAMING
        isClientStreaming && isServerStreaming -> BIDIRECTIONAL_STREAMING
        else -> throw IllegalStateException(
            "Unable to determine cardinality of method: `$fullName`."
        )
    }
