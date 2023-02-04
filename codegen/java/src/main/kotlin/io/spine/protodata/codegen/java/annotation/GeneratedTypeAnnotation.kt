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

import io.spine.protodata.renderer.SourceFile
import javax.annotation.processing.Generated

/**
 * Adds the [javax.annotation.Generated] annotation to the top-level declaration of each Java file
 * in the source set.
 *
 * Deriving classes are likely to inherit the class to pass values to the properties, providing
 * a no-argument constructor, as required for a [Renderer][io.spine.protodata.renderer.Renderer].
 *
 * @see io.spine.protodata.codegen.java.annotation.TypeAnnotation
 */
public open class GeneratedTypeAnnotation(

    /**
     * The name of the code generator as required in [Generated.value].
     *
     * The default value for this parameter is [PROTODATA_CLI].
     */
    protected val generator: String = PROTODATA_CLI,

    /**
     * Tells if the annotated code should have [Generated.date] parameter.
     * If `true`, the value will be set to the instant at local time when
     * the annotation was generated.
     */
    protected val addTimestamp: Boolean = false,

) : TypeAnnotation<Generated>(Generated::class.java) {

    public companion object {

        /**
         * The fully qualified name of the ProtoData command-line application main class.
         */
        public const val PROTODATA_CLI: String = "io.spine.protodata.cli.app.Main"
    }

    override fun renderAnnotationArguments(file: SourceFile): String = "\"$PROTODATA_CLI\""
}
