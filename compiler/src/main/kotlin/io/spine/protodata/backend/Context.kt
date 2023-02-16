/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.base.EventMessage
import io.spine.core.userId
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.BoundedContext.singleTenant
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext

/**
 * A factory for the `Code Generation` bounded context.
 */
public object CodeGenerationContext {

    /**
     * Creates a builder of the bounded context.
     */
    @JvmStatic
    public fun builder(): BoundedContextBuilder = singleTenant("Code Generation").apply {
        add(ViewRepository.default(builtinView()))
        add(ConfigView.Repo())
    }

    @Suppress("UNCHECKED_CAST")
    private fun builtinView(): Class<View<*, *, *>> =
        ProtoSourceFileView::class.java as Class<View<*, *, *>>
}

/**
 * A an external bounded context.
 *
 * This context can emit events which are visible to the `Code Generation` context.
 */
internal sealed class ExternalContext(name: String) : AutoCloseable {

    private val context = ThirdPartyContext.singleTenant(name)
    private val actor = userId { value = name }

    /**
     * Produces and emits events from given event messages.
     */
    fun emitted(events: Sequence<EventMessage>) {
        events.forEach {
            context.emittedEvent(it, actor)
        }
    }

    /**
     * Produces and emits an event from the given event message.
     */
    fun emitted(singleEvent: EventMessage) =
        emitted(sequenceOf(singleEvent))

    override fun close() =
        context.close()
}

/**
 * The `Protobuf Compiler` third-party bounded context.
 */
internal class ProtobufCompilerContext : ExternalContext("Protobuf Compiler")

/**
 * The `ProtoData Configuration` third-party bounded context.
 */
internal class ConfigurationContext : ExternalContext("ProtoData Configuration")
