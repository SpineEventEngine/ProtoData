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
import io.spine.code.proto.FileSet
import io.spine.environment.DefaultMode
import io.spine.protodata.ast.Coordinates
import io.spine.protodata.ast.Documentation
import io.spine.protodata.backend.event.CompilerEvents
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.applyTo
import io.spine.protodata.plugin.render
import io.spine.protodata.protobuf.toPbSourceFile
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.protodata.settings.SettingsDirectory
import io.spine.protodata.type.TypeSystem
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
 * @property id The ID of the pipeline to be used for distinguishing contexts when
 *   two or more pipelines are executed in the same JVM. If not specified, the ID will be generated.
 * @property plugins The code generation plugins to be applied to the pipeline.
 * @property sources The source sets to be processed by the pipeline.
 * @property request The Protobuf compiler request.
 * @property settings The directory to which setting files for the [plugins] should be stored.
 * @property descriptorFilter The predicate to accept descriptors during parsing of the [request].
 *  The default value accepts all the descriptors.
 *  The primary usage scenario for this parameter is accepting only
 *  descriptors of interest when running tests.
 */
@Internal
public class Pipeline(
    public val id: String = generateId(),
    public val plugins: List<Plugin>,
    public val sources: List<SourceFileSet>,
    public val request: CodeGeneratorRequest,
    private val descriptorFilter: DescriptorFilter = { true },
    public val settings: SettingsDirectory
) {

    /**
     * The type system passed to the plugins at the start of the pipeline.
     */
    private val typeSystem: TypeSystem by lazy {
        request.toTypeSystem()
    }

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
        id = id,
        plugins = listOf(plugin),
        sources = listOf(sources),
        request = request,
        settings = settings
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
        clearCaches()
        emitEventsAndRenderSources()
    }

    /**
     * Clears the static caches that could have been created by previous runs, e.g., when
     * running from tests.
     *
     * Clears the caches of previously parsed files to avoid repeated code generation.
     * Also, clears the caches of [Documentation] and [Coordinates] classes.
     */
    private fun clearCaches() {
        SourceFile.clearCache()
        Documentation.clearCache()
        Coordinates.clearCache()
    }

    private fun emitEventsAndRenderSources() {
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
        CodeGenerationContext(id, typeSystem) {
            plugins.forEach {
                it.applyTo(this, typeSystem)
            }
        }

    private fun emitEvents(
        configuration: ConfigurationContext,
        compiler: ProtobufCompilerContext
    ) {
        settings.emitEvents().forEach {
            configuration.emitted(it)
        }
        val events = CompilerEvents.parse(request, descriptorFilter)
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

/**
 * Converts this code generation request into [TypeSystem] taking all the proto files.
 */
private fun CodeGeneratorRequest.toTypeSystem(): TypeSystem {
    val fileDescriptors = FileSet.of(protoFileList).files()
    val protoFiles = fileDescriptors.map { it.toPbSourceFile() }
    return TypeSystem(protoFiles.toSet())
}
