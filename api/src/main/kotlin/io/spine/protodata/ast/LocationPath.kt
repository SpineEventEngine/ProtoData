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
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto.VALUE_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto.METHOD_FIELD_NUMBER
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
import io.spine.util.interlaced

/**
 * A numerical path to a location is source code.
 *
 * Used by the Protobuf compiler as a coordinate system for arbitrary Protobuf declarations.
 *
 * See `google.protobuf.SourceCodeInfo.Location.path` for the explanation of the protocol.
 */
@Suppress("TooManyFunctions")
@JvmInline
internal value class LocationPath
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
                    SERVICE_FIELD_NUMBER,
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

        fun of(option: FieldDescriptor, context: GenericDescriptor): LocationPath {
            val path = when (context) {
                is FileDescriptor -> optionAt(FileDescriptorProto.OPTIONS_FIELD_NUMBER, option)
                is Descriptor -> optionAt(DescriptorProto.OPTIONS_FIELD_NUMBER, option)
                is EnumDescriptor -> optionAt(EnumDescriptorProto.OPTIONS_FIELD_NUMBER, option)
                is ServiceDescriptor -> optionAt(
                    ServiceDescriptorProto.OPTIONS_FIELD_NUMBER,
                    option
                )
                is FieldDescriptor -> optionAt(FieldDescriptorProto.OPTIONS_FIELD_NUMBER, option)
                is OneofDescriptor -> optionAt(OneofDescriptorProto.OPTIONS_FIELD_NUMBER, option)
                is EnumValueDescriptor -> optionAt(
                    EnumValueDescriptorProto.OPTIONS_FIELD_NUMBER,
                    option
                )
                is MethodDescriptor -> optionAt(MethodDescriptorProto.OPTIONS_FIELD_NUMBER, option)
                // Return the non-existing path so that the default `Location` is returned.
                else -> LocationPath(listOf(-1, -1))
            }
            return path
        }

        private fun optionAt(optionsFieldNumber: Int, option: FieldDescriptor): LocationPath =
            LocationPath(listOf(optionsFieldNumber, option.toProto().number))
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

private val Descriptor.isTopLevel: Boolean
    get() = containingType == null

private val EnumDescriptor.isTopLevel: Boolean
    get() = containingType == null
