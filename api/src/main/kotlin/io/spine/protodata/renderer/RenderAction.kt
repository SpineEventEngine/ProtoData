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

package io.spine.protodata.renderer

import io.spine.protodata.Member
import io.spine.protodata.ProtoDeclaration
import io.spine.tools.code.Language

/**
 * A base class for classes that modify the code of a single source file.
 *
 * Render actions participate in the source code rendering process and
 * are called by [Renderer]s either directly or indirectly.
 *
 * @param L the programming language supported by this action.
 * @param P the type of the Protobuf declaration served by this action,
 *   such as [MessageType][io.spine.protodata.MessageType],
 *   [EnumType][io.spine.protodata.EnumType] or [Service][io.spine.protodata.Service].
 * @param language the programming language served by this action.
 * @property subject the Protobuf declaration served by this action.
 * @see Renderer
 */
public abstract class RenderAction<L : Language, P : ProtoDeclaration>
protected constructor(language: L, protected val subject: P) : Member<L>(language) {

    /**
     * Renders the code in the given source file.
     *
     * @param file the source code file to be modified by this action.
     */
    public abstract fun run(file: SourceFile<L>)
}