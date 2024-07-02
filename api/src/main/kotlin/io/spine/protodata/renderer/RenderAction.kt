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

import io.spine.protodata.CodegenContext
import io.spine.protodata.EnumType
import io.spine.protodata.Member
import io.spine.protodata.MessageType
import io.spine.protodata.ProtoDeclaration
import io.spine.protodata.Service
import io.spine.protodata.TypeDeclaration
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
 * @param context the code generation context in which this action runs.
 * @see Renderer
 */
public abstract class RenderAction<L : Language, P : ProtoDeclaration>
protected constructor(
    language: L,
    protected val subject: P,
    context: CodegenContext
) : Member<L>(language) {

    init {
        registerWith(context)
    }

    /**
     * Renders the code in the given source file.
     *
     * @param file the source code file to be modified by this action.
     */
    public abstract fun render(file: SourceFile<L>)
}

/**
 * A render action performed for a Protobuf type,
 * such as [MessageType][io.spine.protodata.MessageType] or [EnumType][io.spine.protodata.EnumType].
 *
 * @param L the programming language supported by this action.
 * @param T the type of the Protobuf declaration served by this action.
 * @param language the programming language served by this action.
 * @property type the same as [subject], added for readability in generated code templates.
 * @param context the code generation context in which this action runs.
 * @see MessageAction
 * @see EnumAction
 */
public abstract class TypeAction<L : Language, T : TypeDeclaration>
protected constructor(
    language: L,
    protected val type: T,
    context: CodegenContext
) : RenderAction<L, T>(language, type, context)

/**
 * A render action performed for a [MessageType][io.spine.protodata.MessageType].
 *
 * @param L the programming language supported by this action.
 * @param language the programming language served by this action.
 * @param type the message type served by this action.
 * @param context the code generation context in which this action runs.
 * @see EnumAction
 * @see ServiceAction
 */
public abstract class MessageAction<L : Language>(
    language: L,
    type: MessageType,
    context: CodegenContext
) : TypeAction<L, MessageType>(language, type, context)

/**
 * A render action performed for an [EnumType][io.spine.protodata.EnumType].
 *
 * @param L the programming language supported by this action.
 * @param language the programming language served by this action.
 * @param type the enum type served by this action.
 * @see MessageAction
 * @see ServiceAction
 */
public abstract class EnumAction<L : Language>(
    language: L,
    type: EnumType,
    context: CodegenContext
) : TypeAction<L, EnumType>(language, type, context)

/**
 * A render action performed for a [Service][io.spine.protodata.Service].
 *
 * @param L the programming language supported by this action.
 * @param language the programming language served by this action.
 * @property service the same as [subject], added for readability in generated code templates.
 * @param context the code generation context in which this action runs.
 * @see MessageAction
 * @see EnumAction
 */
public abstract class ServiceAction<L : Language>(
    language: L,
    protected val service: Service,
    context: CodegenContext
) : RenderAction<L, Service>(language, service, context)
