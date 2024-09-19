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
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import io.spine.annotation.Internal
import io.spine.environment.DefaultMode
import io.spine.protodata.CodegenContext
import io.spine.protodata.backend.event.CompilerEvents
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.applyTo
import io.spine.protodata.plugin.render
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
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
 * the context receive them.
 *
 * Then, the [Renderer]s, which are able to query the states of entities
 * in the `Code Generation` context, alter the source set.
 * This may include creating new files, modifying, or deleting existing ones.
 *
 * Lastly, the source set is stored back onto the file system.
 *
 * @property id the ID of the pipeline to be used for distinguishing contexts when
 *   two or more pipelines are executed in the same JVM. If not specified, the ID will be generated.
 * @property plugins the code generation plugins to be applied to the pipeline.
 * @property sources the source sets to be processed by the pipeline.
 * @property request the Protobuf compiler request.
 * @property settings the directory to which setting files for the [plugins] should be stored.
 */
@Internal
public class Pipeline(
    public val id: String = generateId(),
    public val plugins: List<Plugin>,
    public val sources: List<SourceFileSet>,
    public val request: CodeGeneratorRequest,
    public val settings: SettingsDirectory
) {

    /**
     * Obtains code generation context used by this pipeline.
     */
    @VisibleForTesting
    public val codegenContext: CodegenContext by lazy {
        assembleCodegenContext()
    }

    /**
     * Creates a new `Pipeline` with only one plugin and one source set.
     */
    @VisibleForTesting
    public constructor(
        plugin: Plugin,
        sources: SourceFileSet,
        request: CodeGeneratorRequest,
        settings: SettingsDirectory,
        id: String = generateId()
    ) : this(
        id,
        listOf(plugin),
        listOf(sources),
        request,
        settings
    )

    init {
        under<DefaultMode> {
            use(InMemoryStorageFactory.newInstance())
            use(InMemoryTransportFactory.newInstance())
            use(Delivery.direct())
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
        // Clear the cache of previously parsed files to avoid repeated code generation
        // when running from tests.
        SourceFile.clearCache()

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
        settings.emitEvents().forEach {
            configuration.emitted(it)
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
