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

import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.DescriptorProto.FIELD_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.DescriptorProto.NESTED_TYPE_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.DescriptorProto.ONEOF_DECL_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto.VALUE_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.string.trimWhitespace
import io.spine.util.interlaced

/**
 * Documentation contained in a Protobuf file.
 */
public class Documentation private constructor(locations: List<Location>) {

    private val docs: Map<LocationPath, Location> = locations.associateBy(
        LocationPath.Companion::from
    )

    /**
     * Creates an instance of `Documentation` with all the docs from the given file.
     */
    public constructor(file: FileDescriptorProto) : this(file.sourceCodeInfo.locationList)

    /**
     * Creates an instance of `Documentation` with all the docs from the given file.
     */
    public constructor(file: Descriptors.FileDescriptor) : this(file.toProto())

    /**
     * Obtains documentation for the given message.
     */
    public fun forMessage(d: Descriptor): Doc {
        val path = LocationPath.fromMessage(d)
        return commentsAt(path)
    }

    /**
     * Obtains documentation for the given message field.
     */
    public fun forField(d: FieldDescriptor): Doc {
        val path = LocationPath.fromMessage(d.containingType).field(d)
        return commentsAt(path)
    }

    /**
     * Obtains documentation for the given `oneof` group.
     */
    public fun forOneof(d: OneofDescriptor): Doc {
        val path = LocationPath.fromMessage(d.containingType).oneof(d)
        return commentsAt(path)
    }

    /**
     * Obtains documentation for the given enum type.
     */
    public fun forEnum(d: EnumDescriptor): Doc {
        val path = LocationPath.fromEnum(d)
        return commentsAt(path)
    }

    /**
     * Obtains documentation for the given enum constant.
     */
    public fun forEnumConstant(d: EnumValueDescriptor): Doc {
        val path = LocationPath.fromEnum(d.type).constant(d)
        return commentsAt(path)
    }

    /**
     * Obtains documentation for the given service.
     */
    public fun forService(d: ServiceDescriptor): Doc {
        val path = LocationPath.fromService(d)
        return commentsAt(path)
    }

    /**
     * Obtains documentation for the given RPC method.
     */
    public fun forRpc(d: MethodDescriptor): Doc {
        val path = LocationPath.fromService(d.service).rpc(d)
        return commentsAt(path)
    }

    private fun commentsAt(path: LocationPath): Doc {
        val location = docs[path] ?: Location.getDefaultInstance()
        return doc {
            leadingComment = location.leadingComments.trimWhitespace()
            trailingComment = location.trailingComments.trimWhitespace()
            detachedComment.addAll(location.leadingDetachedCommentsList.trimWhitespace())
        }
    }
}

private fun Iterable<String>.trimWhitespace(): List<String> =
    map { it.trimWhitespace() }

private val Descriptor.isTopLevel: Boolean
    get() = containingType == null

private val EnumDescriptor.isTopLevel: Boolean
    get() = containingType == null

/**
 * A numerical path to a location is source code.
 *
 * Used by the Protobuf compiler as a coordinate system for arbitrary Protobuf declarations.
 *
 * See `google.protobuf.SourceCodeInfo.Location.path` for the explanation of the protocol.
 */
@JvmInline
private value class LocationPath
private constructor(private val value: List<Int>) {

    companion object {

        /**
         * Obtains the `LocationPath` from the Protobuf's `Location`.
         */
        fun from(location: Location): LocationPath {
            return LocationPath(location.pathList)
        }

        /**
         * Obtains the `LocationPath` from the given message.
         */
        fun fromMessage(descriptor: Descriptor): LocationPath {
            val numbers = mutableListOf<Int>()
            numbers.add(MESSAGE_TYPE_FIELD_NUMBER)
            if (!descriptor.isTopLevel) {
                numbers.addAll(upToTop(descriptor.containingType))
                numbers.add(NESTED_TYPE_FIELD_NUMBER)
            }
            numbers.add(descriptor.index)
            return LocationPath(numbers)
        }

        /**
         * Obtains the `LocationPath` from the given enum.
         */
        fun fromEnum(descriptor: EnumDescriptor): LocationPath {
            val numbers = mutableListOf<Int>()
            if (descriptor.isTopLevel) {
                numbers.add(FileDescriptorProto.ENUM_TYPE_FIELD_NUMBER)
            } else {
                numbers.add(MESSAGE_TYPE_FIELD_NUMBER)
                numbers.addAll(upToTop(descriptor.containingType))
                numbers.add(DescriptorProto.ENUM_TYPE_FIELD_NUMBER)
            }
            numbers.add(descriptor.index)
            return LocationPath(numbers)
        }

        /**
         * Obtains the `LocationPath` from the given service.
         */
        fun fromService(descriptor: ServiceDescriptor): LocationPath {
            return LocationPath(
                listOf(
                    FileDescriptorProto.SERVICE_FIELD_NUMBER,
                    descriptor.index
                )
            )
        }

        private fun upToTop(parent: Descriptor): List<Int> {
            val rootPath = mutableListOf<Int>()
            var containingType: Descriptor? = parent
            while (containingType != null) {
                rootPath.add(containingType.index)
                containingType = containingType.containingType
            }
            return rootPath.interlaced(NESTED_TYPE_FIELD_NUMBER)
                .toList()
                .reversed()
        }
    }

    /**
     * Obtains the `LocationPath` to the given field.
     *
     * It's expected that the field belongs to the message located at this location path.
     */
    fun field(field: FieldDescriptor): LocationPath =
        subDeclaration(FIELD_FIELD_NUMBER, field.index)

    /**
     * Obtains the `LocationPath` to the given `oneof` group.
     *
     * It's expected that the group is declared in the message located at this location path.
     */
    fun oneof(group: OneofDescriptor): LocationPath =
        subDeclaration(ONEOF_DECL_FIELD_NUMBER, group.index)

    /**
     * Obtains the `LocationPath` to the given enum constant.
     *
     * It's expected that the constant belongs to the enum located at this location path.
     */
    fun constant(constant: EnumValueDescriptor): LocationPath =
        subDeclaration(VALUE_FIELD_NUMBER, constant.index)

    /**
     * Obtains the `LocationPath` to the given RPC.
     *
     * It's expected that the RPC belongs to the service located at this location path.
     */
    fun rpc(rpc: MethodDescriptor): LocationPath =
        subDeclaration(METHOD_FIELD_NUMBER, rpc.index)

    override fun toString(): String {
        return "LocationPath(${value.joinToString()})"
    }

    private fun subDeclaration(descriptorFieldNumber: Int, index: Int): LocationPath =
        LocationPath(value + arrayOf(descriptorFieldNumber, index))
}
