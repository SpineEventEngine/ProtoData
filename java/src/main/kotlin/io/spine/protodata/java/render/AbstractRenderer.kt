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

package io.spine.protodata.java.render

import com.google.protobuf.Empty
import com.google.protobuf.Message
import io.spine.base.EntityState
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.render.SourceFileSet
import io.spine.reflect.argumentIn

/**
 * An abstract base for Java renderers that takes previously accumulated views as the input.
 *
 * The renderer generates the code by traversing all the view states specified by the generic
 * parameter [V].
 *
 * The code generation process may be tuned by the settings which should be stored
 * before the rendering process starts.
 *
 * @param V The type of the view state which the renderer uses for code generation.
 * @param S The type of the settings used by the renderer. Use [Empty] if settings are not used.
 *
 * @see io.spine.protodata.settings.LoadsSettings
 */
public abstract class AbstractRenderer<V : EntityState<*>, S : Message> : JavaRenderer() {

    /**
     * The source file set with Java files processed by the renderer.
     *
     * This property is not available before the [render] method is called.
     * It is available in the [doRender] method.
     */
    protected lateinit var sources: SourceFileSet

    /**
     * The class matching by the generic parameter [V].
     */
    private val viewClass: Class<V> by lazy {
        genericArgument(0)
    }

    /**
     * The class matching the generic parameter [S].
     */
    private val settingsClass: Class<S> by lazy {
        genericArgument(1)
    }

    /**
     * The [lazily loaded][loadSettings] settings of the plugin.
     *
     * The settings are loaded unless the generic parameter [S] is not [Empty].
     * In such a case, [Empty.getDefaultInstance] is used.
     */
    protected val settings: S by lazy {
        if (settingsClass != Empty::class.java) {
            loadSettings(settingsClass)
        } else {
            @Suppress("UNCHECKED_CAST") // Ensured by `if` above.
            Empty.getDefaultInstance() as S
        }
    }

    /**
     * Tells if the [settings] allow this renderer to work.
     *
     * If this method returns `false` the method [doRender] is never called.
     */
    protected abstract fun isEnabled(settings: S): Boolean

    private fun isRelevant(): Boolean {
        val relevant = sources.hasJavaRoot && isEnabled(settings)
        return relevant
    }

    /**
     * Renders the code traversing all the views calling [doRender], if
     * the rendering is [enabled][isEnabled] by [settings].
     *
     * If the method [isEnabled] returns `false` does nothing.
     */
    override fun render(sources: SourceFileSet) {
        this.sources = sources
        if (!isRelevant()) {
            return
        }
        val views = findViews()
        views.forEach {
            doRender(it)
        }
    }

    private fun findViews(): Set<V> {
        val found = select(viewClass).all()
        return found
    }

    /**
     * Performs code generation in response to the given [view].
     */
    protected abstract fun doRender(view: V)

    /**
     * Obtains a generic argument of a leaf class extending [AbstractRenderer].
     *
     * This way we do not have to pass information about the classes twice: as
     * generic type arguments and as classes passed to constructors.
     */
    private fun <T : Any> genericArgument(index: Int): Class<T> {
        @Suppress("UNCHECKED_CAST")
        return this::class.java.argumentIn<AbstractRenderer<V, S>>(index) as Class<T>
    }
}
