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

package io.spine.protodata.type

import io.spine.protodata.EnumType
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.MessageType
import io.spine.protodata.ProtoDeclaration
import io.spine.protodata.ProtoDeclarationName
import io.spine.protodata.ProtobufDependency
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Service
import io.spine.protodata.ServiceName
import io.spine.protodata.TypeName
import io.spine.server.query.Querying
import io.spine.server.query.select

/**
 * A collection of known Protobuf types.
 */
public class TypeSystem(
    private val files: Set<ProtobufSourceFile>
) {

    public companion object {

        /**
         * Builds a new `TypeSystem` by finding definitions via
         * the given [client][Querying] instance.
         */
        @JvmStatic
        public fun serving(client: Querying): TypeSystem {
            val files = client.select<ProtobufSourceFile>().all()
            val deps = client.select<ProtobufDependency>().all().map { it.file }
            return TypeSystem(files + deps)
        }
    }

    /**
     * Looks up a message type by its name.
     */
    public fun findMessage(name: TypeName): Pair<MessageType, ProtoFileHeader>? =
        find(name) { it.typeMap }

    /**
     * Looks up an enum type by its name.
     */
    public fun findEnum(name: TypeName): Pair<EnumType, ProtoFileHeader>? =
        find(name) { it.enumTypeMap }

    /**
     * Looks up a message or enum type by its name.
     */
    public fun findMessageOrEnum(name: TypeName): Pair<ProtoDeclaration, ProtoFileHeader>? =
        findMessage(name) ?: findEnum(name)

    /**
     * Looks up a service by its name.
     */
    public fun findService(name: ServiceName): Pair<Service, ProtoFileHeader>? =
        find(name) { it.serviceMap }

    private fun <T> find(
        name: ProtoDeclarationName,
        mapSelector: (ProtobufSourceFile) -> Map<String, T>
    ): Pair<T, ProtoFileHeader>? {
        val typeUrl = name.typeUrl
        val file = files.find {
            mapSelector(it).containsKey(typeUrl)
        }
        val types = file?.let(mapSelector)
        val type = types?.get(typeUrl)
        return if (type != null) type to file.file else null
    }
}
