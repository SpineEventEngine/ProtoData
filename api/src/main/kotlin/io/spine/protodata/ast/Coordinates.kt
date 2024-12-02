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

package io.spine.protodata.ast

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.GenericDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.protodata.protobuf.withSourceLines
import io.spine.protodata.util.Cache

/**
 * Provides line and column numbers for declarations in a Protobuf file.
 */
public class Coordinates private constructor(file: FileDescriptorProto) : Locations(file) {

    /**
     * Obtains declaration coordinates the given message.
     */
    public fun forMessage(d: Descriptor): Span {
        val path = LocationPath.fromMessage(d)
        return spanAt(path)
    }

    /**
     * Obtains declaration coordinates the given message.
     */
    public fun forField(d: FieldDescriptor): Span {
        val path = LocationPath.fromMessage(d.containingType).field(d)
        return spanAt(path)
    }

    /**
     * Obtains declaration coordinates for the given `oneof` group.
     */
    public fun forOneof(d: OneofDescriptor): Span {
        val path = LocationPath.fromMessage(d.containingType).oneof(d)
        return spanAt(path)
    }

    /**
     * Obtains declaration coordinates for the given enum type.
     */
    public fun forEnum(d: EnumDescriptor): Span {
        val path = LocationPath.fromEnum(d)
        return spanAt(path)
    }

    /**
     * Obtains declaration coordinates for the given enum constant.
     */
    public fun forEnumConstant(d: EnumValueDescriptor): Span {
        val path = LocationPath.fromEnum(d.type).constant(d)
        return spanAt(path)
    }

    /**
     * Obtains declaration coordinates for the given service.
     */
    public fun forService(d: ServiceDescriptor): Span {
        val path = LocationPath.fromService(d)
        return spanAt(path)
    }

    /**
     * Obtains declaration coordinates for the given RPC method.
     */
    public fun forRpc(d: MethodDescriptor): Span {
        val path = LocationPath.fromService(d.service).rpc(d)
        return spanAt(path)
    }

    private fun spanAt(path: LocationPath): Span {
        val location = locationAt(path)
        return location.toSpan()
    }

    public companion object : Cache<FileDescriptorProto, Coordinates>() {

        override fun create(key: FileDescriptorProto, param: Any?): Coordinates = Coordinates(key)

        /**
         * Obtains coordinates for the given file.
         */
        @JvmStatic
        public fun of(file: FileDescriptor): Coordinates = get(file.toProto(), null)

        /**
         * Obtains coordinates for the given file.
         */
        @JvmStatic
        public fun of(file: FileDescriptorProto): Coordinates = get(file, null)
    }
}

/**
 * Converts the [Location.span][Location.getSpanList] field into [Span].
 *
 * See the documentation of the `Location.span` field for details.
 */
private fun Location.toSpan(): Span {
    if (this == Location.getDefaultInstance()) {
        return Span.getDefaultInstance()
    }
    // Convert the value of the `span` field into four-elements array.
    val slots =
        if (spanCount == 3)
            arrayOf(getSpan(0), getSpan(1), getSpan(0), getSpan(2))
        else
            arrayOf(getSpan(0), getSpan(1), getSpan(2), getSpan(3))
    // Add 1 to make the coordinates 1-based.
    return span {
        startLine = slots[0] + 1
        startColumn = slots[1] + 1
        endLine = slots[2] + 1
        endColumn = slots[3] + 1
    }
}

/**
 * Obtains coordinates for the file this [GenericDescriptor].
 */
internal fun GenericDescriptor.coordinates(): Coordinates {
    val fromResources = withSourceLines()
    return Coordinates.of(fromResources.file)
}
