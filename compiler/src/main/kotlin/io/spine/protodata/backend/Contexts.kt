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

import com.google.common.annotations.VisibleForTesting
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.core.userId
import io.spine.protodata.CodegenContext
import io.spine.protodata.InsertionPointsView
import io.spine.protodata.plugin.ViewRepository
import io.spine.protodata.type.TypeSystem
import io.spine.server.BoundedContext
import io.spine.server.BoundedContext.singleTenant
import io.spine.server.BoundedContextBuilder
import io.spine.server.entity.Entity
import io.spine.server.integration.ThirdPartyContext
import io.spine.server.query.QueryingClient
import kotlin.reflect.jvm.jvmName

/**
 * A factory for the `Code Generation` bounded context.
 */
public class CodeGenerationContext(
    /**
     * The ID of a pipeline which is used to create the context.
     */
    private val pipelineId: String,

    /**
     * An optional setup function for the context.
     */
    setup: BoundedContextBuilder.() -> Unit = { }
) : CodegenContext {

    private val context: BoundedContext

    init {
        val builder = singleTenant("Code Generation-$pipelineId").apply {
            add(ViewRepository.default(ProtoSourceFileView::class.java))
            add(ViewRepository.default(DependencyView::class.java))
            add(ViewRepository.default(InsertionPointsView::class.java))
            add(ConfigView.Repo())
        }
        builder.setup()
        context = builder.build()
    }

    override val typeSystem: TypeSystem by lazy {
        TypeSystem.serving(this)
    }

    /**
     * Lazy initializer for [insertionPointsContext] property.
     *
     * We have the initializer as a separate property to avoid unnecessary creation
     * of `ThirdPartyContext` instance, if [insertionPointsContext] was never called.
     *
     * @see [close]
     */
    private val pointsLazy = lazy {
        ThirdPartyContext.singleTenant("Insertion Points-$pipelineId")
    }

    override val insertionPointsContext: ThirdPartyContext by pointsLazy

    override fun <E : Entity<*, *>> hasEntitiesOfType(cls: Class<E>): Boolean =
        context.hasEntitiesOfType(cls)

    override fun <P : EntityState<*>> select(type: Class<P>): QueryingClient<P> =
        QueryingClient(context, type, this::class.jvmName)

    override fun close() {
        if (pointsLazy.isInitialized()) {
            insertionPointsContext.close()
        }
        context.close()
    }

    public companion object {

        /**
         * Creates a new instance of the `Code Generation` bounded context with
         * only the default repositories.
         */
        @JvmStatic
        @VisibleForTesting
        public fun newInstance(pipelineId: String): CodeGenerationContext =
            CodeGenerationContext(pipelineId)
    }
}

/**
 * An external bounded context.
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
