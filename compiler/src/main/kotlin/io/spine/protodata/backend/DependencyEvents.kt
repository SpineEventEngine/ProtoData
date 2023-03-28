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

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.protodata.EnumType
import io.spine.protodata.FilePath
import io.spine.protodata.MessageType
import io.spine.protodata.ProtoDeclaration
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Service
import io.spine.protodata.enumType
import io.spine.protodata.event.dependencyDiscovered
import io.spine.protodata.messageType
import io.spine.protodata.name
import io.spine.protodata.oneofGroup
import io.spine.protodata.path
import io.spine.protodata.service

/**
 * Creates a `DependencyDiscovered` event from the given file descriptor.
 *
 * The event reflects all the definitions from the file.
 */
internal fun discoverDependencies(fileDescriptor: FileDescriptor) =
    dependencyDiscovered {
        val id = fileDescriptor.path()
        path = id
        file = fileDescriptor.toPbSourceFile()
    }

private fun FileDescriptor.toPbSourceFile(): ProtobufSourceFile {
    val path = path()
    val result = ProtobufSourceFile.newBuilder()
        .setFilePath(path)
        .setFile(toFileWithOptions())
    val doc = Documentation.fromFile(this)
    with(DefinitionsBuilder(path, doc)) {
        result.putAllType(
            messageTypes().associateByName()
        )
        result.putAllEnumType(
            enumTypes().associateByName()
        )
        result.putAllService(
            services().associateByName()
        )
    }
    return result.build()
}

private fun <T : ProtoDeclaration> Sequence<T>.associateByName() =
    associateBy { it.name.typeUrl() }

/**
 * A builder for the Protobuf definitions of a single `.proto` file.
 *
 * @property path the relative file path to the Protobuf file
 * @property documentation all the documentation and comments present in the file
 */
private class DefinitionsBuilder(
    private val path: FilePath,
    private val documentation: Documentation,
) {

    /**
     * Builds the message type definitions.
     *
     * @return all the message types declared in the file, including nested types.
     */
    fun FileDescriptor.messageTypes(): Sequence<MessageType> {
        var messages = messageTypes.asSequence()
        for (msg in messageTypes) {
            messages += walkMessage(msg) { it.nestedTypes }
        }
        return messages.map { it.asMessage() }
    }

    /**
     * Builds the enum type definitions.
     *
     * @return all the enums declared in the file, including nested enums.
     */
    fun FileDescriptor.enumTypes(): Sequence<EnumType> {
        var enums = enumTypes.asSequence()
        for (msg in messageTypes) {
            enums += walkMessage(msg) { it.enumTypes }
        }
        return enums.map { it.asEnum() }
    }

    /**
     * Builds the service definitions.
     *
     * @return all the services declared in the file, including the nested ones.
     */
    fun FileDescriptor.services(): Sequence<Service> =
        services.asSequence().map { it.asService() }

    private fun Descriptor.asMessage() = messageType {
        val typeName = name()
        name = typeName
        file = path
        doc = documentation.forMessage(this@asMessage)
        option.addAll(listOptions(options))
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        oneofGroup.addAll(realOneofs.map { oneofGroup {
            val groupName = it.name()
            name = groupName
            field.addAll(it.fields.map { f -> f.buildFieldWithOptions(typeName, documentation) })
            option.addAll(listOptions(options))
            doc = documentation.forOneof(it)
        }
        })
        field.addAll(fields.map { it.buildFieldWithOptions(typeName, documentation) })
        nestedMessages.addAll(nestedTypes.map { it.name() })
        nestedEnums.addAll(enumTypes.map { it.name() })
    }

    private fun EnumDescriptor.asEnum() = enumType {
        val typeName = name()
        name = typeName
        option.addAll(listOptions(options))
        file = path
        constant.addAll(values.map { it.buildConstantWithOptions(typeName, documentation) })
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        doc = documentation.forEnum(this@asEnum)
    }

    private fun ServiceDescriptor.asService() = service {
        val serviceName = name()
        name = serviceName
        file = path
        rpc.addAll(methods.map { it.buildRpcWithOptions(serviceName, documentation) })
        option.addAll(listOptions(options))
        doc = documentation.forService(this@asService)
    }
}

/**
 * Produces a sequence by walking through all the nested message definitions staring with `type`.
 *
 * @param type the message definition which may contain nested message definition to walk through
 * @param extractorFun a function that, given a message definition, extracts the items of interest
 * @return results of the calls to [extractorFun] flattened into one sequence
 */
private fun <T> walkMessage(
    type: Descriptor,
    extractorFun: (Descriptor) -> Iterable<T>,
): Sequence<T> {
    val queue = ArrayDeque<Descriptor>()
    queue.add(type)
    return sequence {
        while (queue.isNotEmpty()) {
            val msg = queue.removeFirst()
            yieldAll(extractorFun(msg))
            queue.addAll(msg.nestedTypes)
        }
    }
}
