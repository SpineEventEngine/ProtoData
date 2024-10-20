/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.plugin.add
import io.spine.protodata.type.TypeSystem
import io.spine.server.BoundedContext
import io.spine.server.BoundedContext.singleTenant
import io.spine.server.BoundedContextBuilder
import io.spine.server.entity.Entity
import io.spine.server.integration.ThirdPartyContext
import io.spine.server.query.QueryingClient
import kotlin.reflect.jvm.jvmName

/**
 * The `Code Generation` context is responsible for "hosting" custom views, repositories,
 * and other components of a `BoundedContext` that a user-defined plugin can
 * [add][io.spine.protodata.plugin.Plugin.extend] to a code generation [Pipeline].
 *
 * Views that are available in this context by default are:
 *  * [ProtoSourceFileView] — the view on the source files of the Protobuf model.
 *  * [DependencyView] — the view on the dependencies of the Protobuf model.
 *  * [InsertionPointsView] — the view on the insertion points defined in the current [Pipeline].
 *  * [SettingsView] — the view on the user data configuration.
 */
public class CodeGenerationContext(
    /**
     * The ID of a pipeline which is used to create the context.
     */
    private val pipelineId: String,

    override val typeSystem: TypeSystem,

    /**
     * An optional setup function for the context.
     */
    setup: BoundedContextBuilder.() -> Unit = { }
) : CodegenContext {

    /**
     * Creates a stub instance of the context with empty [TypeSystem].
     *
     * This is a test-only constructor for the cases when resolving of types is unnecessary.
     */
    @VisibleForTesting
    public constructor(pipelineId: String) : this(pipelineId, TypeSystem(emptySet()))

    /**
     * The underlying instance of the `Code Generation` bounded context.
     */
    public val context: BoundedContext

    init {
        val builder = singleTenant("$NAME_PREFIX$pipelineId").apply {
            add<ProtoSourceFileView>()
            add<DependencyView>()
            add<InsertionPointsView>()
            add(SettingsView.Repo())
        }
        builder.setup()
        context = builder.build()
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

    override fun isOpen(): Boolean = context.isOpen

    override fun close() {
        if (pointsLazy.isInitialized()) {
            insertionPointsContext.closeIfOpen()
        }
        context.closeIfOpen()
    }

    public companion object {

        /**
         * The prefix for the context names.
         *
         * The prefix is followed by the [pipelineId] in the generated context name.
         */
        @VisibleForTesting
        public const val NAME_PREFIX: String = "Code Generation-"

        /**
         * Creates a new instance of the `Code Generation` bounded context with
         * only the default repositories.
         */
        @JvmStatic
        @JvmOverloads
        @VisibleForTesting
        public fun newInstance(
            pipelineId: String = Pipeline.generateId()
        ): CodeGenerationContext = CodeGenerationContext(pipelineId)
    }
}

/**
 * An external bounded context.
 *
 * This context can emit events which are visible to the `Code Generation` context.
 */
internal sealed class ExternalContext(pipelineId: String, name: String) : AutoCloseable {

    private val context = ThirdPartyContext.singleTenant("$name-$pipelineId")
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

    override fun close() {
        if (context.isOpen) {
            context.close()
        }
    }
}

/**
 * The `Protobuf Compiler` third-party bounded context.
 */
internal class ProtobufCompilerContext(pipelineId: String) :
    ExternalContext(pipelineId, "Protobuf Compiler")

/**
 * The `ProtoData Configuration` third-party bounded context.
 */
internal class ConfigurationContext(pipelineId: String) :
    ExternalContext(pipelineId, "ProtoData Configuration")
