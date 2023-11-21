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

package io.spine.protodata.codegen.java

import io.spine.protodata.File
import io.spine.protodata.ProtoFileHeader
import io.spine.protodata.ProtobufSourceFile
import io.spine.protodata.TypeName
import io.spine.protodata.renderer.Renderer
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
        val file = findFile(declaredIn)
        return type.javaClassName(file)
    }

    /**
     * Obtains the path the `.java` file generated from the given type.
     *
     * The path is relative to the generated source root. This path is useful for finding source
     * files in a [SourceSet][io.spine.protodata.renderer.SourceFileSet].
     */
    protected fun javaFileOf(type: TypeName, declaredIn: File): Path {
        val file = findFile(declaredIn)
        return type.javaFile(file)
    }

    private fun findFile(path: File): ProtoFileHeader =
        select(ProtobufSourceFile::class.java)
            .findById(path)!!
            .header
}
