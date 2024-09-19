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

package io.spine.protodata.render

import com.google.protobuf.Any
import com.google.protobuf.Empty
import com.google.protobuf.Message
import io.spine.protobuf.AnyPacker
import io.spine.protodata.CodegenContext
import io.spine.protodata.ProtoDeclaration
import io.spine.reflect.Factory
import io.spine.tools.code.Language
import org.checkerframework.checker.signature.qual.FqBinaryName

/**
 * Creates instances of [RenderAction] specified in the given [actions].
 *
 * The main use case for this is to create rendering actions for
 * a [io.spine.protodata.renderer.Renderer] which obtains the instance of [Actions]
 * from code generation settings.
 *
 * The classes names of which are specified in the given [actions] must satisfy
 * the following criteria:
 *  1. Be `public` so that this factory can create their instances reflectively.
 *  2. Serve the language defined by the generic parameter [L].
 *  3. Serve the Protobuf declaration specified by the generic parameter [D].
 *
 * In addition to the criteria above, if an action accepts a parameter, its class must
 * have a `public` constructor which accepts four parameters with the following types:
 *  1. [D] with an instance of Protobuf declaration to be served by the action.
 *  2. `SourceFile<L>` — the file to be handled by the action.
 *  3. [Message][com.google.protobuf.Message] or a derived class which corresponds to the
 *   [packed parameter][com.google.protobuf.Any]
 *   [associated][io.spine.protodata.settings.ActionsKt.Dsl.action] with the class name.
 *  4. [CodegenContext].
 *
 * For action classes that do not accept parameters, the `public` constructor must
 * have three parameters:
 *  1. [D] with an instance of Protobuf declaration to be served by the action.
 *  2. `SourceFile<L>` — the file to be handled by the action.
 *  3. [CodegenContext].
 *
 * @property language The language the created actions will serve.
 * @property actions The rendering actions to create.
 * @param classLoader The class loader to obtain action classes.
 *
 * @see Actions
 * @see io.spine.protodata.settings.add
 */
public class ActionFactory<L : Language, D : ProtoDeclaration>(
    private val language: L,
    private val actions: io.spine.protodata.render.Actions,
    classLoader: ClassLoader
) : Factory<RenderAction<L, D, *>>(classLoader) {

    init {
        require(actions.actionMap.isNotEmpty()) {
            "No actions types are passed to the factory."
        }
    }

    /**
     * Creates instances of [RenderAction]
     *
     * @throws ActionFactoryException if an error occurred during the creation of actions.
     */
    public fun create(
        declaration: D,
        file: SourceFile<L>,
        context: CodegenContext
    ): List<RenderAction<L, D, *>> {
        val result = buildList {
            actions.actionMap.forEach { (className, packedParameter) ->
                val parameter = packedParameter.unpackParameter()
                val action = tryCreate(className, declaration, file, parameter, context)
                add(action)
            }
        }
        return result
    }

    private fun tryCreate(
        className: @FqBinaryName String,
        declaration: D,
        file: SourceFile<L>,
        parameter: Message,
        context: CodegenContext
    ): RenderAction<L, D, *> {
        @Suppress("TooGenericExceptionCaught") // Intentionally.
        val action = try {
            if (parameter is Empty) {
                create(className, declaration, file, context)
            } else {
                create(className, declaration, file, parameter, context)
            }
        } catch (e: Throwable) {
            ActionFactoryException.propagate(className, e)
        }
        checkMatchingLanguage(className, action)
        return action
    }

    /**
     * Verifies if the language served by the [action] is the same of the one
     * for which the factory is created or is a subtype of the language.
     *
     * @throws ActionFactoryException if the language of the action is not compatible with
     *   the language of the factory.
     */
    private fun checkMatchingLanguage(
        className: @FqBinaryName String,
        action: RenderAction<L, D, out Message>
    ) {
        val actionLanguage = action.language
        if (!language::class.java.isInstance(actionLanguage)) {
            ActionFactoryException.incompatibleLanguage(className, language, actionLanguage)
        }
    }
}

/**
 * Unpacks this instance of `Any` following the convention for parameterless
 * rendering actions defined by the [Actions] type.
 *
 * @see Actions.getActionMap
 */
private fun Any.unpackParameter(): Message {
    if (this == Any.getDefaultInstance()) {
        return Empty.getDefaultInstance()
    }
    return AnyPacker.unpack(this)
}
