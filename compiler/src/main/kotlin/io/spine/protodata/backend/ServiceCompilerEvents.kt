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
import io.spine.base.EventMessage
import io.spine.protodata.File
import io.spine.protodata.Service
import io.spine.protodata.ServiceName
import io.spine.protodata.event.RpcEntered
import io.spine.protodata.event.RpcExited
import io.spine.protodata.event.RpcOptionDiscovered
import io.spine.protodata.event.ServiceEntered
import io.spine.protodata.event.ServiceExited
import io.spine.protodata.event.ServiceOptionDiscovered
import io.spine.protodata.name

/**
 * Produces events for a service.
 */
internal class ServiceCompilerEvents(
    private val file: File,
    private val documentation: Documentation
) {

    /**
     * Yields compiler events for the given service.
     *
     * Opens with an [ServiceEntered] event. Then go the events regarding the service metadata.
     * Then go the events regarding the RPC methods. At last, closes with an [ServiceExited] event.
     */
    internal suspend fun SequenceScope<EventMessage>.produceServiceEvents(
        descriptor: Descriptors.ServiceDescriptor
    ) {
        val path = file.path
        val serviceName = descriptor.name()
        val service = Service.newBuilder()
            .setName(serviceName)
            .setDoc(documentation.forService(descriptor))
            .setFile(path)
            .build()
        yield(
            ServiceEntered.newBuilder()
                .setFile(path)
                .setService(service)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            ServiceOptionDiscovered.newBuilder()
                .setFile(path)
                .setService(serviceName)
                .setOption(it)
                .build()
        }
        descriptor.methods.forEach { produceRpcEvents(serviceName, it) }
        yield(
            ServiceExited.newBuilder()
                .setFile(path)
                .setService(serviceName)
                .build()
        )
    }

    private suspend fun SequenceScope<EventMessage>.produceRpcEvents(
        service: ServiceName,
        descriptor: Descriptors.MethodDescriptor
    ) {
        val path = file.path
        val rpc = buildRpc(descriptor, service, documentation)
        yield(
            RpcEntered.newBuilder()
                .setFile(path)
                .setService(service)
                .setRpc(rpc)
                .build()
        )
        produceOptionEvents(descriptor.options) {
            RpcOptionDiscovered.newBuilder()
                .setFile(path)
                .setService(service)
                .setRpc(rpc.name)
                .setOption(it)
                .build()
        }
        yield(
            RpcExited.newBuilder()
                .setFile(path)
                .setService(service)
                .setRpc(rpc.name)
                .build()
        )
    }
}
