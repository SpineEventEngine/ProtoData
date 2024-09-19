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

import com.google.protobuf.Message
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.spine.protodata.MessageType
import io.spine.protodata.context.CodegenContext
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java

/**
 * A code generation action on the message class itself.
 *
 * @param P The type of the parameter passed to the action.
 *
 * @param type The type of the message.
 * @param file The source code to which the action is applied.
 * @param parameter The parameter passed to the action.
 * @param context The code generation context in which this action runs.
 */
public abstract class DirectMessageAction<P : Message>(
    type: MessageType,
    file: SourceFile<Java>,
    parameter: P,
    context: CodegenContext
) : MessageAction<P>(type, file, parameter, context) {

    /**
     * The message class located in the [file].
     */
    final override val cls: PsiClass by lazy {
        val f = file.psi() as PsiJavaFile
        f.findClass(messageClass)
    }
}
