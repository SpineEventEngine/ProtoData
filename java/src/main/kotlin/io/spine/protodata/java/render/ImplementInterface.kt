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

import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.createClassReference
import io.spine.tools.psi.java.implement

/**
 * An abstract base for code generation actions that make a message class
 * implement the given superinterface.
 *
 * @param type The type of the message.
 * @param file The source code to which the action is applied.
 * @param superInterface The interface to implement.
 * @param context The code generation context in which this action runs.
 */
public open class ImplementInterface(
    type: MessageType,
    file: SourceFile<Java>,
    superInterface: SuperInterface,
    context: CodegenContext
) : DirectMessageAction<SuperInterface>(type, file, superInterface, context) {

    override fun doRender() {
        val ref = interfaceReference()
        val si = elementFactory.createClassReference(ref, parameter.genericArgumentList, cls)
        cls.implement(si)
    }

    /**
     * Calculates the reference to the interface to be used in the code.
     *
     * If the interface is in the same package with the class, uses simple name(s).
     * Otherwise, uses the canonical name.
     */
    private fun interfaceReference(): String {
        val iface = ClassName.guess(parameter.name)
        val theClass = ClassName.guess(cls.qualifiedName ?: cls.name!!)
        return if (theClass.packageName == iface.packageName)
            iface.simpleNames.joinToString(".")
        else
            iface.canonical
    }
}
