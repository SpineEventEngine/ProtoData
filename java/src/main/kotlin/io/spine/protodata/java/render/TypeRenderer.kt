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

import io.spine.base.EntityState
import io.spine.protodata.ast.MessageType
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.TypeActions
import io.spine.tools.code.Java
import io.spine.tools.psi.java.execute

/**
 * The abstract base for renderers running one or more render actions on a message type.
 *
 * The type and actions are obtained from a view implementing [TypeActions].
 * The renderer acts on all the views queried by their [viewClass].
 *
 * @param V The type of the view state which the renderer uses for code generation.
 *
 * @see TypeListRenderer
 */
public abstract class TypeRenderer<V>  : BaseRenderer<V>()
        where V : EntityState<*>, V : TypeActions {

    override fun doRender(view: V) {
        val type = view.getType()
        val sourceFile = sources.javaFileOf(type)
        execute {
            doRender(view, type, sourceFile)
        }
    }

    private fun doRender(view: V, type: MessageType, sourceFile: SourceFile<Java>) {
        val actions = RenderActions(type, sourceFile, view.getActions(), context!!)
        actions.apply()
    }
}
