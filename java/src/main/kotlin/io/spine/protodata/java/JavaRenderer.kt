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

package io.spine.protodata.java

import io.spine.protodata.File
import io.spine.protodata.MessageType
import io.spine.protodata.TypeName
import io.spine.protodata.findHeader
import io.spine.protodata.qualifiedName
import io.spine.protodata.render.Renderer
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.tools.code.Java
import java.nio.file.Path

/**
 * A [Renderer] which generates Java code.
 */
public abstract class JavaRenderer : Renderer<Java>(Java) {

    /**
     * Obtains the [ClassName] of the given Protobuf type.
     */
    protected fun classNameOf(type: TypeName, declaredIn: File): ClassName {
        val header = findHeader(declaredIn)
        return type.javaClassName(header!!)
    }

    /**
     * Obtains the path the `.java` file generated from the given type.
     *
     * The path is relative to the generated source root. This path is useful for finding source
     * files in a [SourceSet][io.spine.protodata.renderer.SourceFileSet].
     */
    protected fun javaFileOf(type: TypeName, declaredIn: File): Path {
        val header = findHeader(declaredIn)!!
        return type.javaFile(header)
    }

    /**
     * Locates a Java source file for the given message in this [SourceFileSet].
     *
     * @throws IllegalStateException
     *          if there is no Java file for the given message type in this source file set.
     */
    protected fun SourceFileSet.javaFileOf(msg: MessageType): SourceFile<Java> {
        val javaFile = javaFileOf(type = msg.name, declaredIn = msg.file)
        val sourceFile = find(javaFile)
        check(sourceFile != null) {
            "Unable to locate the file for the message type `${msg.name.qualifiedName}`" +
                    " in the source set `$this`."
        }
        @Suppress("UNCHECKED_CAST") // Safe because we look for a Java file.
        return sourceFile as SourceFile<Java>
    }
}
