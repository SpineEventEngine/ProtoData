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

import io.spine.protodata.MessageType
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.render.ActionFactory
import io.spine.protodata.render.Actions
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java

/**
 * Runs code generation actions for the given [type].
 *
 * @property type The message type for which code generation is performed.
 * @property file The file with the Java class with the message type.
 * @property actions Rendering actions to be applied to the [type].
 * @property context The code generation context of the operation.
 *
 * @see TypeRenderer
 * @see TypeListRenderer
 */
public class RenderActions(
    private val type: MessageType,
    private val file: SourceFile<Java>,
    private val actions: Actions,
    private val context: CodegenContext
) {
    private val classloader = Thread.currentThread().contextClassLoader
    private val factory = ActionFactory<Java, MessageType>(Java, actions, classloader)

    /**
     * Applies code generation to the [file].
     */
    public fun apply() {
        val acs = factory.create(type, file, context)
        acs.forEach {
            it.render()
        }
    }
}
