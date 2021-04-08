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
import io.spine.server.ServerEnvironment
import io.spine.server.storage.memory.InMemoryStorageFactory

/**
 * A pipeline which processes the Protobuf files.
 *
 * A pipeline consists of the `ProtoData` context, which receives Protobuf compiler events, and
 * a single [Renderer]. A pipeline runs on a single source set.
 *
 * The pipeline starts by building the `ProtoData` bounded context with the supplied
 * [Plugin]s. Then, the Protobuf compiler events are emitted and the subscribers in
 * the context receive them. Then, the [Renderer], which is able to query the states of entities in
 * the `ProtoData` context, alters the source set. This may include creating new files and/or
 * modifying existing ones. Lastly, the source set is stored onto the file system.
 */
public class Pipeline(
    private val extensions: List<Plugin>,
    private val renderer:  Renderer,
    private val sourceSet: SourceSet,
    private val request: CodeGeneratorRequest
) : Logging {

    init {
        val config = ServerEnvironment.`when`(Production::class.java)
        config.use(InMemoryStorageFactory.newInstance())
    }

    /**
     * Executes the processing pipeline.
     */
    public operator fun invoke() {
        val contextBuilder = ProtoDataContext.builder()
        extensions.forEach { it.fillIn(contextBuilder) }
        val context = contextBuilder.build()

        val events = CompilerEvents.parse(request)
        ProtobufCompilerContext.emitted(events)

        renderer.protoDataContext = context
        val enhanced = renderer.render(sourceSet)
        enhanced.files.forEach {
            it.write()
        }
        context.close()
    }
}
