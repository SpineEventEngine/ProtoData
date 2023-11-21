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

package io.spine.protodata.backend.event

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.EnumDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.OneofDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.protodata.EnumType
import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.ProtoDeclaration
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Service
import io.spine.protodata.TypeName
import io.spine.protodata.backend.Documentation
import io.spine.protodata.enumType
import io.spine.protodata.event.dependencyDiscovered
import io.spine.protodata.file
import io.spine.protodata.messageType
import io.spine.protodata.name
import io.spine.protodata.oneofGroup
import io.spine.protodata.protobufSourceFile
import io.spine.protodata.service

/**
 * Creates a `DependencyDiscovered` event from the given file descriptor.
 *
 * The event reflects all the definitions from the file.
 */
internal fun discoverDependencies(fileDescriptor: FileDescriptor) =
    dependencyDiscovered {
        val id = fileDescriptor.file()
        path = id
        source = fileDescriptor.toPbSourceFile()
    }

private fun FileDescriptor.toPbSourceFile(): ProtobufSourceFile {
    val path = file()
    val doc = Documentation.fromFile(this)
    val definitions = DefinitionFactory(this, path, doc)
    return protobufSourceFile {
        file = path
        header = toFileWithOptions()
        with(definitions) {
            type.putAll(messageTypes().associateByUrl())
            enumType.putAll(enumTypes().associateByUrl())
            service.putAll(services().associateByUrl())
        }
    }
}

private fun <T : ProtoDeclaration> Sequence<T>.associateByUrl() =
    associateBy { it.name.typeUrl }

/**
 * A factory of Protobuf definitions of a single `.proto` file.
 *
 * @property path the relative file path to the Protobuf file
 * @property documentation all the documentation and comments present in the file
 */
private class DefinitionFactory(
    private val file: FileDescriptor,
    private val path: File,
    private val documentation: Documentation,
) {

    /**
     * Builds the message type definitions from the [file].
     *
     * @return all the message types declared in the file, including nested types.
     */
    fun messageTypes(): Sequence<MessageType> {
        var messages = file.messageTypes.asSequence()
        for (msg in file.messageTypes) {
            messages += walkMessage(msg) { it.nestedTypes }
        }
        return messages.map { it.asMessage() }
    }

    /**
     * Builds the enum type definitions from the [file].
     *
     * @return all the enums declared in the file, including nested enums.
     */
    fun enumTypes(): Sequence<EnumType> {
        var enums = file.enumTypes.asSequence()
        for (msg in file.messageTypes) {
            enums += walkMessage(msg) { it.enumTypes }
        }
        return enums.map { it.asEnum() }
    }

    /**
     * Builds the service definitions from the [file].
     *
     * @return all the services declared in the file, including the nested ones.
     */
    fun services(): Sequence<Service> =
        file.services.asSequence().map { it.asService() }

    /**
     * Converts the receiver `Descriptor` into a [MessageType].
     */
    private fun Descriptor.asMessage() = messageType {
        val typeName = name()
        name = typeName
        file = path
        doc = documentation.forMessage(this@asMessage)
        option.addAll(options.toList())
        if (containingType != null) {
            declaredIn = containingType.name()
        }
        oneofGroup.addAll(realOneofs.map { it.asOneof(typeName) })
        field.addAll(listFields(fields, typeName))
        nestedMessages.addAll(nestedTypes.map { it.name() })
        nestedEnums.addAll(enumTypes.map { it.name() })
    }

    private fun OneofDescriptor.asOneof(typeName: TypeName) = oneofGroup {
        val groupName = name()
        name = groupName
        field.addAll(listFields(fields, typeName))
        option.addAll(options.toList())
        doc = documentation.forOneof(this@asOneof)
    }

    private fun EnumDescriptor.asEnum() = enumType {
        val typeName = name()
        name = typeName
        option.addAll(options.toList())
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
        option.addAll(options.toList())
        doc = documentation.forService(this@asService)
    }

    private fun listFields(descriptors: Iterable<FieldDescriptor>, declaringType: TypeName) =
        descriptors.map { f -> f.buildFieldWithOptions(declaringType, documentation) }
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
