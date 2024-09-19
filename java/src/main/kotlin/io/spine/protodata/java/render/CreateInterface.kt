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

import io.spine.protodata.java.ClassName
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.tools.code.Java
import kotlin.io.path.exists

/**
 * Creates a new Java file with the interface with the given [name].
 *
 * The created interface will extend the [superInterface] if it is passed to the constructor.
 *
 * @param name The name of the interface to be created.
 * @param superInterface Optional base type for the new interface to extend.
 * @throws IllegalArgumentException If the given interface name is nested.
 */
public class CreateInterface(
    private val name: ClassName,
    private val superInterface: SuperInterface? = null
) {
    init {
        require(name.isNested.not()) {
            "The interface `$name` must not be nested."
        }
    }

    /**
     * Creates a new Java file for the interface using its qualified [name] for calculating
     * the path in the given [source file set][sources].
     *
     * @param sources The source file set in which to add the new file.
     * @throws IllegalStateException If the file already exists.
     */
    public fun render(sources: SourceFileSet): SourceFile<Java> {
        val targetFile = sources.outputRoot.resolve(name.javaFile)
        check(targetFile.exists().not()) {
            "The source file `$targetFile` already exists."
        }
        val code = compose()
        @Suppress("UNCHECKED_CAST") /* The type is ensured by the file extension. */
        val file = sources.createFile(name.javaFile, code) as SourceFile<Java>
        return file
    }

    private fun compose(): String {
        val packageBlock =
            if (name.packageName.isEmpty()) ""
            else "package ${name.packageName};\n\n"

        val extendsBlock =
            if (superInterface == null) ""
            else "extends ${superInterface.reference} "

        val fullCode =
            packageBlock +
            """
            public interface ${name.simpleName} $extendsBlock{
            }
            """.trimIndent()
        return fullCode
    }
}

private val SuperInterface.reference: String
    get() {
        val genericArgs = genericArgumentList.run {
            if (isEmpty()) ""
            else "<${joinToString(", ")}>"
        }
        return "${name}$genericArgs"
    }
