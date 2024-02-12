/*
 * Copyright 2024, TeamDev. All rights reserved.
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

package io.spine.protodata

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.option.OptionsProto
import io.spine.protodata.CallCardinality.BIDIRECTIONAL_STREAMING
import io.spine.protodata.CallCardinality.CLIENT_STREAMING
import io.spine.protodata.CallCardinality.SERVER_STREAMING
import io.spine.protodata.CallCardinality.UNARY

/**
 * Obtains the relative path to this file as a [File].
 */
public fun FileDescriptor.file(): File = file { path = name }

/**
 * Obtains the name of this message type as a [TypeName].
 */
public fun Descriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Obtains the name of this enum type as a [TypeName].
 */
public fun EnumDescriptor.name(): TypeName = buildTypeName(name, file, containingType)

/**
 * Obtains the name of this service as a [ServiceName].
 */
public fun ServiceDescriptor.name(): ServiceName = serviceName {
    typeUrlPrefix = file.typeUrlPrefix
    packageName = file.`package`
    simpleName = name
}

/**
 * Obtains the name of this field as a [FieldName].
 */
public fun FieldDescriptor.name(): FieldName = fieldName { value = name }

/**
 * Obtains the name of this `oneof` as a [OneofName].
 */
public fun OneofDescriptor.name(): OneofName = oneofName { value = name }

/**
 * Obtains the name of this RPC method as an [RpcName].
 */
public fun MethodDescriptor.name(): RpcName = rpcName { value = name }

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

private val FileDescriptor.typeUrlPrefix: String
    get() {
        val customTypeUrl = options.getExtension(OptionsProto.typeUrlPrefix)
        return if (customTypeUrl.isNullOrBlank()) {
            "type.googleapis.com"
        } else {
            customTypeUrl
        }
    }

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
