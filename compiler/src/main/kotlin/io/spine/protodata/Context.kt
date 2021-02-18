/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.protodata

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.base.Production
import io.spine.core.UserId
import io.spine.protodata.subscriber.Subscriber
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder
import io.spine.server.ServerEnvironment
import io.spine.server.integration.ThirdPartyContext
import io.spine.server.storage.memory.InMemoryStorageFactory
import io.spine.server.transport.memory.InMemoryTransportFactory

/**
 * A factory for the `ProtoData` bounded context.
 */
public object ProtoDataContext {

    /**
     * Creates the instance of the bounded context.
     */
    public fun build(vararg withSubscribers: Subscriber<*>) : BoundedContext {
        return builder(*withSubscribers).build()
    }

    @VisibleForTesting
    internal fun builder(vararg withSubscribers: Subscriber<*>): BoundedContextBuilder {
        val config = ServerEnvironment.`when`(Production::class.java)
        config.use(InMemoryTransportFactory.newInstance())
        config.use(InMemoryStorageFactory.newInstance())

        val builder = BoundedContext
            .singleTenant("ProtoData")
            .add(ProtoSourceFileRepository())
        withSubscribers.forEach { builder.addEventDispatcher(it) }
        return builder
    }
}

/**
 * The `Protobuf Compiler` third-party bounded context.
 */
public object ProtobufCompilerContext {

    private const val NAME = "Protobuf Compiler"

    private val context = ThirdPartyContext.singleTenant(NAME)
    private val actor = UserId
        .newBuilder()
        .setValue(NAME)
        .build()

    /**
     * Produces and emits compiler events describing the types listed in
     * the [CodeGeneratorRequest.getFileToGenerateList].
     *
     * The request must contain descriptors for the files to generate, as well as for their
     * dependencies.
     */
    public fun emittedEventsFor(request: CodeGeneratorRequest) {
        val events = CompilerEvents.parse(request)
        events.forEach { context.emittedEvent(it, actor) }
    }
}
