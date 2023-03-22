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

import com.google.protobuf.Descriptors
import com.google.protobuf.compiler.PluginProtos
import io.spine.code.proto.FileSet
import io.spine.protodata.EnumType
import io.spine.protodata.FilePath
import io.spine.protodata.MessageType
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.Service
import io.spine.protodata.enumType
import io.spine.protodata.event.CompilerEvent
import io.spine.protodata.event.FileEntered
import io.spine.protodata.event.FileExited
import io.spine.protodata.event.FileOptionDiscovered
import io.spine.protodata.event.dependencyDiscovered
import io.spine.protodata.file
import io.spine.protodata.messageType
import io.spine.protodata.name
import io.spine.protodata.oneofGroup
import io.spine.protodata.path
import io.spine.protodata.service
import io.spine.protodata.typeUrl

/**
 * A factory for Protobuf compiler events.
 */
public object CompilerEvents {

    /**
     * Produces a sequence of events based on the given descriptor set.
     *
     * The sequence is produced lazily. An element is produced only when polled.
     *
     * The resulting sequence is always finite, it's limited by the type set.
     */
    public fun parse(request: PluginProtos.CodeGeneratorRequest): Sequence<CompilerEvent> {
        val filesToGenerate = request.fileToGenerateList.toSet()
        val files = FileSet.of(request.protoFileList)
        return sequence {
            val (ownFiles, dependencies) = files.files()
                .partition { it.name in filesToGenerate }
            yieldAll(dependencies.map(::toDependencyEvent))
            ownFiles
                .map { ProtoFileEvents(it, it.name in filesToGenerate) }
                .forEach { it.apply { produceFileEvents() } }
        }
    }

    private fun toDependencyEvent(fileDescriptor: Descriptors.FileDescriptor) =
        dependencyDiscovered {
            val id = fileDescriptor.path()
            file = id
            content = fileDescriptor.toPbSourceFile()
        }
}

private fun Descriptors.FileDescriptor.toPbSourceFile(): ProtobufSourceFile {
    val path = path()
    val result = ProtobufSourceFile.newBuilder()
        .setFilePath(path)
        .setFile(toFile())
    val doc = Documentation.fromFile(this)
    result.putAllType(
        messageTypes(path, doc)
            .map { it.name.typeUrl() to it }
            .toMap()
    )
    result.putAllEnumType(
        enumTypes(path, doc)
            .map { it.name.typeUrl() to it }
            .toMap()
    )
    result.putAllService(
        services(path, doc)
            .map { it.name.typeUrl() to it }
            .toMap()
    )
    return result.build()
}

private fun Descriptors.FileDescriptor.messageTypes(
    path: FilePath,
    doc: Documentation
): Sequence<MessageType> {
    var messages = messageTypes.asSequence()
    for (msg in messageTypes) {
        messages += walkMessage(msg) { it.nestedTypes }
    }
    return messages.map { it.asMessage(path, doc) }
}

private fun Descriptors.FileDescriptor.enumTypes(
    path: FilePath,
    doc: Documentation
): Sequence<EnumType> {
    var enums = enumTypes.asSequence()
    for (msg in messageTypes) {
        enums += walkMessage(msg) { it.enumTypes }
    }
    return enums.map { it.asEnum(path, doc) }
}

private fun Descriptors.FileDescriptor.services(
    path: FilePath,
    doc: Documentation
): Sequence<Service> = services.asSequence().map { it.asService(path, doc) }

private fun Descriptors.Descriptor.asMessage(
    path: FilePath,
    documentation: Documentation
) = messageType {
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
    }})
    field.addAll(fields.map { it.buildFieldWithOptions(typeName, documentation) })
    nestedMessages.addAll(nestedTypes.map { it.name() })
    nestedEnums.addAll(enumTypes.map { it.name() })
}

private fun Descriptors.EnumDescriptor.asEnum(
    path: FilePath, documentation: Documentation
) = enumType {
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

private fun Descriptors.ServiceDescriptor.asService(
    path: FilePath, documentation: Documentation
) = service {
    val serviceName = name()
    name = serviceName
    rpc.addAll(methods.map { it.buildRpcWithOptions(serviceName, documentation) })
    option.addAll(listOptions(options))
    doc = documentation.forService(this@asService)

}

private fun <T> walkMessage(
    type: Descriptors.Descriptor,
    extractorFun: (Descriptors.Descriptor) -> Iterable<T>
): Sequence<T> {
    val queue = ArrayDeque<Descriptors.Descriptor>()
    queue.add(type)
    return sequence {
        while (queue.isNotEmpty()) {
            val msg = queue.removeFirst()
            yieldAll(extractorFun(msg))
            queue.addAll(msg.nestedTypes)
        }
    }
}

private fun Descriptors.FileDescriptor.toFile() = file {
    path = path()
    packageName = `package`
    syntax = this@toFile.syntax.toSyntaxVersion()
}

/**
 * Produces events from the associated file.
 */
private class ProtoFileEvents(
    private val fileDescriptor: Descriptors.FileDescriptor,
    private val shouldGenerate: Boolean = true
) {

    private val file = fileDescriptor.toFile()

    private val documentation = Documentation.fromFile(fileDescriptor)

    /**
     * Yields compiler events for the given file.
     *
     * Opens with an [FileEntered] event. Then go the events regarding the file metadata. Then go
     * the events regarding the file contents. At last, closes with an [FileExited] event.
     */
    suspend fun SequenceScope<CompilerEvent>.produceFileEvents() {
        yield(
            FileEntered.newBuilder()
                .setPath(file.path)
                .setFile(file)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
        produceOptionEvents(fileDescriptor.options) {
            FileOptionDiscovered.newBuilder()
                .setFile(file.path)
                .setOption(it)
                .setGenerationRequested(shouldGenerate)
                .build()
        }
        val messageEvents = MessageCompilerEvents(file, documentation, shouldGenerate)
        fileDescriptor.messageTypes.forEach {
            messageEvents.apply { produceMessageEvents(it) }
        }
        val enumEvents = EnumCompilerEvents(file, documentation, shouldGenerate)
        fileDescriptor.enumTypes.forEach {
            enumEvents.apply { produceEnumEvents(it) }
        }
        val serviceEvents = ServiceCompilerEvents(file, documentation, shouldGenerate)
        fileDescriptor.services.forEach {
            serviceEvents.apply { produceServiceEvents(it) }
        }
        yield(
            FileExited.newBuilder()
                .setFile(file.path)
                .setGenerationRequested(shouldGenerate)
                .build()
        )
    }
}
