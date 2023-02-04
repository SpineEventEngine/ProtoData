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
import io.spine.protodata.renderer.SourceFileSet
import java.lang.annotation.ElementType.TYPE
import java.lang.annotation.Target

public abstract class TypeAnnotation<T : Annotation>(
    public val annotationClass: Class<T>
) : JavaRenderer() {

    init {
        checkAnnotationTargetsType()
    }

    override fun render(sources: SourceFileSet) {
        TODO("Not yet implemented")
    }

    /**
     * Renders the code for passing arguments for the [annotationClass].
     *
     * If there are no arguments to pass, the overriding method must return an empty string.
     */
    protected abstract fun renderAnnotationParameters(): String

    /**
     * Ensures that the [annotationClass] passed to the constructor:
     *   1. Is annotated with [@Target][Target].
     *   2. The annotation has [TYPE] as one of the targets.
     */
    private fun checkAnnotationTargetsType() {
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
