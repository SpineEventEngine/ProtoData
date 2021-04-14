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

package io.spine.protodata.renderer

import io.spine.base.EntityState
import io.spine.protodata.QueryingClient
import io.spine.server.BoundedContext

/**
 * A `Renderer` takes an existing source set, modifies it, including changing the contents of
 * existing source files or creating new ones, and renders the resulting code into a [SourceSet].
 *
 * Instances of `Renderer`s are created via reflection. It is required that the concrete classes
 * have a `public` no-argument constructor.
 */
public abstract class Renderer
protected constructor(internal val supportedInsertionPoints: Set<InsertionPoint> = setOf()) {

    internal lateinit var protoDataContext: BoundedContext

    /**
     * Processes the given `sources` and produces the updated [SourceSet].
     *
     * If a file is present in the input source set but not the output, the file is left untouched.
     * If a file is present in the output source set but not the input, the file created.
     * If a file is present is both the input and the output source sets, the file is overridden.
     */
    public abstract fun render(sources: SourceSet)

    /**
     * Creates a [QueryingClient] to find projections of the given class.
     *
     * Users may create their own projections and register them in the `Code Generation` context via
     * a [Plugin][io.spine.protodata.Plugin].
     *
     * This method is targeted for Java API users. If you use Kotlin, see the no-param overload for
     * prettier code.
     */
    protected fun <P : EntityState> select(type: Class<P>): QueryingClient<P> {
        return QueryingClient(protoDataContext, type, javaClass.name)
    }

    /**
     * Creates a [QueryingClient] to find projections of the given type.
     *
     * Users may create their own projections and register them in the `Code Generation` context via
     * a [Plugin][io.spine.protodata.Plugin].
     */
    protected inline fun <reified P : EntityState> select(): QueryingClient<P> {
        val cls = P::class.java
        return select(cls)
    }
}
