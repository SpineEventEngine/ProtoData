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

import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import io.spine.base.EventMessage
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.buildRpc
import io.spine.protodata.event.ServiceEntered
import io.spine.protodata.event.ServiceExited
import io.spine.protodata.event.rpcEntered
import io.spine.protodata.event.rpcExited
import io.spine.protodata.event.rpcOptionDiscovered
import io.spine.protodata.event.serviceDiscovered
import io.spine.protodata.event.serviceEntered
import io.spine.protodata.event.serviceExited
import io.spine.protodata.event.serviceOptionDiscovered
import io.spine.protodata.name
import io.spine.protodata.produceOptionEvents
import io.spine.protodata.toService

/**
 * Produces events for a service.
 */
internal class ServiceCompilerEvents(
    private val header: ProtoFileHeader
) {

    /**
     * Yields compiler events for the given service.
     *
     * Opens with an [ServiceEntered] event. Then go the events regarding the service metadata.
     * Then go the events regarding the RPC methods. At last, closes with an [ServiceExited] event.
     */
    internal suspend fun SequenceScope<EventMessage>.produceServiceEvents(
        desc: ServiceDescriptor
    ) {
        val path = header.file
        val serviceType = desc.toService()
        yield(
            serviceDiscovered {
                file = path
                service = serviceType
            }
        )
        val serviceName = desc.name()
        yield(
            serviceEntered {
                file = path
                service = serviceName
            }
        )
        produceOptionEvents(desc.options) {
            serviceOptionDiscovered {
                file = path
                service = serviceName
                option = it
            }
        }
        desc.methods.forEach {
            produceRpcEvents(it)
        }
        yield(
            serviceExited {
                file = path
                service = serviceName
            }
        )
    }

    private suspend fun SequenceScope<EventMessage>.produceRpcEvents(
        desc: MethodDescriptor
    ) {
        val serviceName = desc.service.name()
        val path = header.file
        val theRpc = buildRpc(desc, serviceName)
        yield(
            rpcEntered {
                file = path
                service = serviceName
                rpc = theRpc
            }
        )
        produceOptionEvents(desc.options) {
            rpcOptionDiscovered {
                file = path
                service = serviceName
                rpc = theRpc.name
                option = it
            }
        }
        yield(
            rpcExited {
                file = path
                service = serviceName
                rpc = theRpc.name
            }
        )
    }
}
