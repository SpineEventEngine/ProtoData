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
package io.spine.protodata.codegen.java.annotation

import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.file.BeforePrimaryDeclaration
import io.spine.protodata.renderer.SourceFile
import io.spine.protodata.renderer.SourceFileSet
import java.lang.annotation.ElementType.TYPE
import java.lang.annotation.Target

/**
 * A [JavaRenderer] which annotates a Java type using the given [annotation][annotationClass].
 *
 * The implementation assumes that [PrintBeforePrimaryDeclaration][io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration]
 * renderer is inserted before a reference to a renderer derived from this class.
 */
public abstract class TypeAnnotation<T : Annotation>(
    protected val annotationClass: Class<T>
) : JavaRenderer() {

    init {
        checkAnnotationClass()
    }

    final override fun render(sources: SourceFileSet) {
        sources.forEach { file ->
            file.at(BeforePrimaryDeclaration).add(
                annotationText(file)
            )
        }
    }

    private fun annotationText(file: SourceFile): String {
        return "@${annotationClassReference()}${annotationArguments(file)}"
    }

    private fun annotationClassReference(): String {
        val qualifiedName = annotationClass.name
        return if (qualifiedName.contains("java.lang")) {
            annotationClass.simpleName
        } else {
            qualifiedName
        }
    }

    private fun annotationArguments(file: SourceFile): String {
        val args = renderAnnotationArguments(file)
        return if (args.isEmpty()) {
            return ""
        } else {
            "($args)"
        }
    }

    /**
     * Renders the code for passing arguments for the [annotationClass].
     *
     * If there are no arguments to pass, the overriding method must return an empty string.
     */
    protected abstract fun renderAnnotationArguments(file: SourceFile): String

    /**
     * Ensures that the [annotationClass] passed to the constructor satisfy the following criteria:
     *   1. The class is annotated with [@Target][Target].
     *   2. The annotation has [TYPE] as one of the targets.
     *
     * If one of these criteria is not met, [IllegalArgumentException] will be thrown.
     */
    private fun checkAnnotationClass() {
        val targetClass = Target::class.java
        require(annotationClass.isAnnotationPresent(targetClass)) {
            "The annotation class `${annotationClass.name}`" +
                    " should have `${targetClass.name}`."
        }
        val targets = annotationClass.getAnnotation(targetClass)
        require(targets.value.contains(TYPE)) {
            "Targets of `${annotationClass.name}` do not include ${TYPE.name}."
        }
    }
}
