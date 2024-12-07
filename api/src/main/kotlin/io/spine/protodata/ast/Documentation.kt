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
import io.spine.string.trimWhitespace

/**
 * Documentation contained in a Protobuf file.
 */
public class Documentation private constructor(file: FileDescriptorProto) : Locations(file) {

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

    /**
     * Obtains documentation for the given option.
     *
     * @param option The descriptor of the option.
     * @param context The descriptor of the scope in which the option is declared, such as
     *   a message type, an enumeration, a service, etc.
     */
    public fun forOption(option: FieldDescriptor, context: GenericDescriptor): Doc {
        val path = LocationPath.of(option, context)
        return commentsAt(path)
    }

    private fun commentsAt(path: LocationPath): Doc {
        val location = locationAt(path)
        return doc {
            leadingComment = location.leadingComments.trimWhitespace()
            trailingComment = location.trailingComments.trimWhitespace()
            detachedComment.addAll(location.leadingDetachedCommentsList.trimWhitespace())
        }
    }

    public companion object : Cache<FileDescriptorProto, Documentation>() {

        override fun create(key: FileDescriptorProto, param: Any?): Documentation =
            Documentation(key)

        /**
         * Obtains documentation for the given file.
         */
        @JvmStatic
        public fun of(file: FileDescriptor): Documentation = get(file.toProto())

        /**
         * Obtains documentation for the given file.
         */
        @JvmStatic
        public fun of(file: FileDescriptorProto): Documentation = get(file)
    }
}

private fun Iterable<String>.trimWhitespace(): List<String> =
    map { it.trimWhitespace() }

/**
 * Obtains documentation for the file of this [GenericDescriptor].
 */
internal fun GenericDescriptor.documentation(): Documentation {
    val fromResources = withSourceLines()
    return Documentation.of(fromResources.file)
}
