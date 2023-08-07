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

package io.spine.protodata

import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.applyTo
import io.spine.protodata.renderer.Renderer
import io.spine.protodata.renderer.SourceFileSet
import io.spine.protodata.type.TypeNameConvention
import io.spine.protodata.type.TypeNameElement
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder

public interface Module<out N : TypeNameElement> {

    public val renderers: List<Renderer>

    public val plugins: List<Plugin>

    public val nameConventions: Set<TypeNameConvention<N>>
}

public class ImplicitModule(
    override val plugins: List<Plugin>,
    override val renderers: List<Renderer>
) : Module<TypeNameElement> {

    override val nameConventions: Set<TypeNameConvention<TypeNameElement>> = emptySet()
}

public fun <N : TypeNameElement> Module<N>.applyPlugins(codeGenContext: BoundedContextBuilder) {
    plugins.forEach {
        it.applyTo(codeGenContext)
    }
}

public fun <N : TypeNameElement> Module<N>.render(
    codegenContext: BoundedContext,
    sources: Iterable<SourceFileSet>
) {
    renderers.forEach { r ->
        r.registerWith(codegenContext)
        sources.forEach(r::renderSources)
    }
}
