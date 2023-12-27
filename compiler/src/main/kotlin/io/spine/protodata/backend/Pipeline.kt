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

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.annotation.Internal
import io.spine.environment.DefaultMode
import io.spine.protodata.CodegenContext
import io.spine.protodata.backend.event.CompilerEvents
import io.spine.protodata.config.Configuration
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.applyTo
import io.spine.protodata.plugin.render
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.server.delivery.Delivery
import io.spine.server.storage.memory.InMemoryStorageFactory
import io.spine.server.transport.memory.InMemoryTransportFactory
import io.spine.server.under


/**
 * A pipeline which processes the Protobuf files.
 *
 * A pipeline consists of the `Code Generation` context, which receives Protobuf compiler events,
 * and one or more [Renderer]s.
 *
 * The pipeline starts by building the `Code Generation` bounded context with the supplied
 * [Plugin]s. Then, the Protobuf compiler events are emitted and the subscribers in
 * the context receive them. Then, the [Renderer]s, which are able to query the states of entities
 * in the `Code Generation` context, alters the source set. This may include creating new files,
 * modifying, or deleting existing ones. Lastly, the source set is stored back onto the file system.
 */
@Internal
public class Pipeline(
    /**
     * The ID of the pipeline to be used for distinguishing contexts when
     * two or more pipelines are executed in the same JVM.
     */
    public val id: String = generateId(),

    /**
     * The code generation plugins to be applied to the pipeline.
     */
    private val plugins: List<Plugin>,

    /**
     * The source sets to be processed by the pipeline.
     */
    private val sources: List<SourceFileSet>,

    /**
     * The Protobuf compiler request.
     */
    private val request: CodeGeneratorRequest,

    /**
     * The configuration of the pipeline.
     */
    private val config: Configuration? = null
) {

    private lateinit var codegenContext: CodegenContext

    /**
     * Creates a new `Pipeline` with only one plugin and one source set.
     */
    @VisibleForTesting
    public constructor(
        plugin: Plugin,
        sources: SourceFileSet,
        request: CodeGeneratorRequest,
        config: Configuration? = null,
        id: String = generateId()
    ) : this(
        id,
        listOf(plugin),
        listOf(sources),
        request,
        config
    )

    init {
        under<DefaultMode> {
            use(InMemoryStorageFactory.newInstance())
            use(InMemoryTransportFactory.newInstance())
        }
    }

    /**
     * Executes the processing pipeline.
     *
     * The execution is performed in [Delivery.direct] mode, meaning
     * that no concurrent modification of entity states is allowed.
     * Therefore, the execution of the code related to the signal processing
     * should be single-threaded.
     */
    public operator fun invoke() {
        under<DefaultMode> {
            use(Delivery.direct())
        }
        codegenContext = assembleCodegenContext()
        codegenContext.use {
            ConfigurationContext(id).use { configuration ->
                ProtobufCompilerContext(id).use { compiler ->
                    emitEvents(configuration, compiler)
                    renderSources()
                }
            }
        }
    }

    /**
     * Assembles the `Code Generation` context by applying given [plugins].
     */
    private fun assembleCodegenContext(): CodegenContext =
        CodeGenerationContext(id) {
            plugins.forEach {
                it.applyTo(this)
            }
        }

    private fun emitEvents(
        configuration: ConfigurationContext,
        compiler: ProtobufCompilerContext
    ) {
        config?.let {
            val event = it.produceEvent()
            configuration.emitted(event)
        }
        val events = CompilerEvents.parse(request)
        compiler.emitted(events)
    }

    private fun renderSources() {
        plugins.forEach { it.render(codegenContext, sources) }
        sources.forEach { it.write() }
    }

    public companion object {

        /**
         * Generates a random ID for the pipeline.
         *
         * The generated ID is guaranteed to be unique for the current JVM.
         */
        @JvmStatic
        public fun generateId(): String = SecureRandomString.generate()
    }
}
