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
import io.spine.base.Production
import io.spine.logging.Logging
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceSet
import io.spine.protodata.subscriber.CodeEnhancement
import io.spine.protodata.subscriber.SkipEverything
import io.spine.protodata.subscriber.Subscriber
import io.spine.protodata.transport.PrunableTransport
import io.spine.server.BoundedContext
import io.spine.server.ServerEnvironment
import io.spine.server.projection.ProjectionRepository
import io.spine.server.storage.memory.InMemoryStorageFactory

/**
 * A pipeline which processes the Protobuf files.
 *
 * A pipeline consists of several [Subscriber]s and a single [Renderer] and runs on a single
 * source set.
 *
 * The pipeline starts by building a Bounded Context with the supplied subscribers.
 * Then, the Protobuf compiler events are emitted for the subscribers to listen. Subscribers produce
 * [CodeEnhancement]s in response to the events.
 * Then, the [Renderer], based on the generated enhancements, alters the source set. This may
 * include creating new files and/or modifying existing ones.
 * Lastly, the source set is stored onto the file system.
 */
public class Pipeline(
    private val subscribers: List<Subscriber<*>>,
    private val projections: List<ProjectionRepository<*, *, *>>,
    private val renderer: (List<CodeEnhancement>) -> Renderer,
    private val sourceSet: SourceSet,
    private val request: CodeGeneratorRequest
) : Logging, AutoCloseable {

    private var generatorContext: BoundedContext? = null

    init {
        val config = ServerEnvironment.`when`(Production::class.java)
        config.use(PrunableTransport)
        config.use(InMemoryStorageFactory.newInstance())
    }

    /**
     * Executes the processing pipeline.
     */
    public operator fun invoke() {
        val enhancements = processProtobuf()
        if (SkipEverything !in enhancements) {
            val enhanced = renderer(enhancements).render(sourceSet)
            enhanced.files.forEach {
                it.write()
            }
        } else {
            _info().log("Skipping everything. ${enhancements.size - 1} code enhancements ignored.")
        }
    }

    private fun processProtobuf(): List<CodeEnhancement> {
        val protoDataContext = ProtoDataContext.build(projections)
        val events = CompilerEvents.parse(request)
        ProtobufCompilerContext.emitted(events)
        PrunableTransport.prune()
        generatorContext = ProtoDataGeneratorContext.build(subscribers, protoDataContext)
        ProtobufCompilerContext.emitted(events)
        return subscribers.flatMap { it.producedEnhancements }
    }

    override fun close() {
        generatorContext?.close()
    }
}
