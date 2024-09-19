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

import com.google.protobuf.StringValue
import com.google.protobuf.stringValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.spine.protodata.CodegenContext
import io.spine.protodata.MessageType
import io.spine.protodata.render.SourceFile
import io.spine.tools.code.Java
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.addFirst
import io.spine.tools.psi.java.addLast
import io.spine.tools.psi.java.createPrivateConstructor
import io.spine.tools.psi.java.makeFinal
import io.spine.tools.psi.java.makePublic
import io.spine.tools.psi.java.makeStatic

/**
 * Abstract base for code generators creating classes nested into Java code of message types.
 *
 * @param type the type of the message.
 * @param file the source code to which the action is applied.
 * @property simpleName a simple name of the nested class to be generated.
 * @param context the code generation context in which this action runs.
 */
public abstract class CreateNestedClass(
    type: MessageType,
    file: SourceFile<Java>,
    protected val simpleName: String,
    context: CodegenContext
) : MessageAction<StringValue>(type, file, stringValue { value = simpleName }, context) {

    /**
     * The target of the code generation action.
     */
    protected override val cls: PsiClass by lazy {
        createClass()
    }

    private fun createClass(): PsiClass {
        val c = elementFactory.createClass(simpleName)
        c.commonSetup()
        return c
    }

    /**
     * A callback to tune the [cls] in addition to the actions performed during
     * the lazy initialization of the property.
     */
    protected abstract fun tuneClass()

    /**
     * A callback for creating a Javadoc comment of the class produced by this factory.
     *
     * Implementing methods may use [messageJavadocRef] to reference the class for which
     * this factory produces a nested class [cls].
     *
     * @return full text of the Javadoc comment to be created for the class, or
     *         an empty string if the comment is unnecessary.
     */
    protected abstract fun classJavadoc(): String

    /**
     * Creates the constructor for the class.
     *
     * Default implementation creates a parameterless private constructor.
     */
    protected open fun createConstructor(cls: PsiClass): PsiMethod {
        val ctor = elementFactory.createPrivateConstructor(
            cls,
            javadocLine = "Prevents instantiation of this class."
        )
        return ctor
    }

    /**
     * Calls [tuneClass] and the inserts the tuned class into the message class.
     */
    protected override fun doRender() {
        tuneClass()
        val targetClass = psiFile.findClass(messageClass)
        targetClass.addLast(cls)
    }

    private fun PsiClass.commonSetup() {
        makePublic().makeStatic().makeFinal()
        val ctor = createConstructor(this)
        addLast(ctor)
        addAnnotation()
        addClassJavadoc()
    }

    /**
     * Generates an annotation to be added for the created class.
     *
     * Overriding methods may return custom annotation or `null`, if no annotation is necessary.
     */
    protected abstract fun createAnnotation(): PsiAnnotation?

    private fun PsiClass.addAnnotation() {
        val annotation = createAnnotation()
        annotation?.let {
            addFirst(it)
        }
    }

    private fun PsiClass.addClassJavadoc() {
        val text = classJavadoc()
        if (text.isNotEmpty()) {
            val classJavadoc = elementFactory.createDocCommentFromText(text, null)
            addFirst(classJavadoc)
        }
    }

    override fun toString(): String {
        return "CreateNestedClass(type=$type, file=$file, simpleName=\"$simpleName\")"
    }
}
