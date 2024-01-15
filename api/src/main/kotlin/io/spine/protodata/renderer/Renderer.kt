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

package io.spine.protodata.renderer

import io.spine.annotation.Internal
import io.spine.base.EntityState
import io.spine.protodata.CodegenContext
import io.spine.protodata.ContextAware
import io.spine.protodata.settings.LoadsSettings
import io.spine.protodata.type.TypeSystem
import io.spine.server.query.QueryingClient
import io.spine.tools.code.Language

/**
 * A `Renderer` takes an existing [SourceFileSet] and modifies it,
 * changing the contents of existing source files, creating new ones, or
 * deleting unwanted files.
 *
 * Instances of `Renderer`s are usually created by
 * the [Plugin.renderers()][io.spine.protodata.plugin.Plugin.renderers] method.
 */
public abstract class Renderer<L : Language>
protected constructor(
    protected val language: L
) : LoadsSettings, ContextAware {

    private lateinit var _context: CodegenContext

    protected val context: CodegenContext?
        get() = if (this::_context.isInitialized) {
            _context
        } else {
            null
        }

    /**
     * A type system with the Protobuf types defined in the current code generation pipeline.
     *
     * Is `null` if the type system is not yet available to this renderer.
     *
     * This property is guaranteed to be non-`null` in [renderSources].
     */
    protected val typeSystem: TypeSystem?
        get() = context?.typeSystem

    /**
     * Performs required changes to the given source set.
     */
    public fun renderSources(sources: SourceFileSet) {
        val relevantFiles = sources.subsetWhere { language.matches(it.relativePath) }
        relevantFiles.prepareForQueries(this)
        render(relevantFiles)
        sources.mergeBack(relevantFiles)
    }

    /**
     * Makes changes to the given source set.
     *
     * The source set is guaranteed to consist only of the files, containing the code in
     * the supported programming [language].
     *
     * This method may be called several times, if ProtoData is called with multiple source and
     * target directories.
     */
    protected abstract fun render(sources: SourceFileSet)

    /**
     * Creates a [QueryingClient] for obtaining entity states of the given type.
     *
     * @param S
     *         the type of the entity state.
     */
    public inline fun <reified S : EntityState<*>> select(): QueryingClient<S> =
        select(S::class.java)

    /**
     * Creates a [QueryingClient] for obtaining entity states of the given type.
     *
     * @param S
     *         the type of the entity state.
     * @param type
     *         the class of the entity state.
     */
    public final override fun <S : EntityState<*>> select(type: Class<S>): QueryingClient<S> =
        _context.select(type)

    final override fun <T: Any> loadSettings(cls: Class<T>): T = super.loadSettings(cls)

    final override fun settingsAvailable(): Boolean = super.settingsAvailable()

    /**
     * Injects the `Code Generation` context into this renderer.
     *
     * The reference to the context is needed to query the state of entities.
     *
     * This method is `public` but is essentially `internal` to ProtoData SDK.
     * It is not supposed to be called by authors of [Renderer]s directly.
     *
     * @see [select]
     * @see [io.spine.protodata.backend.Pipeline]
     */
    @Internal
    public override fun registerWith(context: CodegenContext) {
        if (isRegistered()) {
            check(_context == context) {
                "Unable to register the renderer `$this` with" +
                        " `${context}`." +
                        " The renderer is already registered with" +
                        " `${this._context}`."
            }
            return
        }
        _context = context
    }

    @Internal
    override fun isRegistered(): Boolean {
        return this::_context.isInitialized
    }
}
