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

import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.base.EventMessage
import io.spine.code.proto.FileSet
import io.spine.protodata.backend.Documentation
import io.spine.protodata.event.FileEntered
import io.spine.protodata.event.FileExited
import io.spine.protodata.event.FileOptionDiscovered

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
    public fun parse(request: CodeGeneratorRequest): Sequence<EventMessage> {
        val filesToGenerate = request.fileToGenerateList.toSet()
        val files = FileSet.of(request.protoFileList)
        return sequence {
            val (ownFiles, dependencies) = files.files().partition {
                it.name in filesToGenerate
            }
            yieldAll(dependencies.map(::discoverDependencies))
            ownFiles
                .map(::ProtoFileEvents)
                .forEach { it.apply { produceFileEvents() } }
        }
    }
}

/**
 * Produces events from the associated file.
 */
private class ProtoFileEvents(
    private val fileDescriptor: FileDescriptor
) {

    private val header = fileDescriptor.toHeader()

    private val documentation = Documentation.fromFile(fileDescriptor)

    /**
     * Yields compiler events for the given file.
     *
     * Opens with an [FileEntered] event. Then go the events regarding the file metadata. Then go
     * the events regarding the file contents. At last, closes with an [FileExited] event.
     */
    suspend fun SequenceScope<EventMessage>.produceFileEvents() {
        yield(
            FileEntered.newBuilder()
                .setPath(header.file)
                .setHeader(header)
                .build()
        )
        produceOptionEvents(fileDescriptor.options) {
            FileOptionDiscovered.newBuilder()
                .setFile(header.file)
                .setOption(it)
                .build()
        }
        val messageEvents = MessageCompilerEvents(header, documentation)
        fileDescriptor.messageTypes.forEach {
            messageEvents.apply { produceMessageEvents(it) }
        }
        val enumEvents = EnumCompilerEvents(header, documentation)
        fileDescriptor.enumTypes.forEach {
            enumEvents.apply { produceEnumEvents(it) }
        }
        val serviceEvents = ServiceCompilerEvents(header, documentation)
        fileDescriptor.services.forEach {
            serviceEvents.apply { produceServiceEvents(it) }
        }
        yield(
            FileExited.newBuilder()
                .setFile(header.file)
                .build()
        )
    }
}
