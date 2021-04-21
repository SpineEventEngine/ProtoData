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

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.base.EventMessage
import io.spine.core.UserId
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext

/**
 * A factory for the `Code Generation` bounded context.
 */
internal object CodeGenerationContext {

    /**
     * Creates a builder of the bounded context.
     */
    fun builder(): BoundedContextBuilder {
        val builder = BoundedContext
            .singleTenant("Code Generation")
        builder.add(ViewRepository.default(ProtoSourceFileView::class))
        return builder
    }
}

/**
 * The `Protobuf Compiler` third-party bounded context.
 */
internal object ProtobufCompilerContext {

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
    fun emitted(events: Sequence<EventMessage>) {
        events.forEach {
            context.emittedEvent(it, actor)
        }
    }
}
